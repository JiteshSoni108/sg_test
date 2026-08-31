package com.example.requestmanagement.service;

import com.example.requestmanagement.domain.RequestAuditEntity;
import com.example.requestmanagement.domain.RequestEntity;
import com.example.requestmanagement.domain.RequestState;
import com.example.requestmanagement.exception.BusinessRuleViolationException;
import com.example.requestmanagement.exception.NotFoundException;
import com.example.requestmanagement.repository.RequestAuditRepository;
import com.example.requestmanagement.repository.RequestRepository;
import com.example.requestmanagement.state.RequestStateMachine;
import com.example.requestmanagement.web.AuditResponse;
import com.example.requestmanagement.web.CreateRequest;
import com.example.requestmanagement.web.RequestResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class RequestService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "name", "state");

    private final RequestRepository requestRepository;
    private final RequestAuditRepository auditRepository;
    private final RequestStateMachine stateMachine;
    private final PublicationNumberGenerator publicationNumberGenerator;

    public RequestService(RequestRepository requestRepository, RequestAuditRepository auditRepository, RequestStateMachine stateMachine, PublicationNumberGenerator publicationNumberGenerator) {

        this.requestRepository = requestRepository;
        this.auditRepository = auditRepository;
        this.stateMachine = stateMachine;
        this.publicationNumberGenerator = publicationNumberGenerator;
    }

    /**
     * Creates a new request in CREATED state.
     */
    @Transactional
    @RateLimiter(name = "requestWrite")
    @Bulkhead(name = "requestWrite", type = Bulkhead.Type.SEMAPHORE)
    public RequestResponse create(CreateRequest request, String actor) {

        validateActor(actor);

        String name = normalizeRequired(request.name(), "Request name is required");

        String content = normalizeRequired(request.content(), "Request content is required");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        RequestEntity entity = new RequestEntity(name, content, now);

        requestRepository.save(entity);

        auditRepository.save(RequestAuditEntity.created(entity.getId(), RequestState.CREATED, actor, now));

        return RequestResponse.from(entity);
    }

    /**
     * Retrieves a request by ID.
     * <p>
     * Cached for performance.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "requests", key = "#id")
    @RateLimiter(name = "requestRead")
    @Bulkhead(name = "requestRead", type = Bulkhead.Type.SEMAPHORE)
    public RequestResponse get(long id) {

        validatePositiveId(id);

        return RequestResponse.from(getEntity(id));
    }

    /**
     * Searches requests using optional name and state filters.
     */
    @Transactional(readOnly = true)
    @RateLimiter(name = "requestRead")
    @Bulkhead(name = "requestRead", type = Bulkhead.Type.SEMAPHORE)
    public Page<RequestResponse> search(String name, RequestState state, Pageable pageable) {

        Specification<RequestEntity> specification = Specification.allOf();

        if (name != null && !name.isBlank()) {

            String normalizedName = name.trim().toLowerCase(Locale.ROOT);

            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + normalizedName + "%"));
        }

        if (state != null) {

            specification = specification.and((root, query, cb) -> cb.equal(root.get("state"), state));
        }

        Pageable normalizedPageable = normalizePageable(pageable);

        return requestRepository.findAll(specification, normalizedPageable).map(RequestResponse::from);
    }

    /**
     * Updates request content.
     * <p>
     * Allowed only in CREATED or VERIFIED state.
     */
    @Transactional
    @CacheEvict(cacheNames = "requests", key = "#id")
    @RateLimiter(name = "requestWrite")
    @Bulkhead(name = "requestWrite", type = Bulkhead.Type.SEMAPHORE)
    public RequestResponse updateContent(long id, String content, String actor) {

        validatePositiveId(id);
        validateActor(actor);

        String normalizedContent = normalizeRequired(content, "Request content is required");

        RequestEntity entity = getEntity(id);

        validateContentEditable(entity);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        RequestState currentState = entity.getState();

        entity.updateContent(normalizedContent, now);

        auditRepository.save(RequestAuditEntity.contentUpdated(entity.getId(), currentState, actor, now));

        return RequestResponse.from(entity);
    }

    /**
     * Moves CREATED -> VERIFIED.
     */
    @Transactional
    @CacheEvict(cacheNames = "requests", key = "#id")
    @RateLimiter(name = "requestWrite")
    @Bulkhead(name = "requestWrite", type = Bulkhead.Type.SEMAPHORE)
    public RequestResponse verify(long id, String actor) {

        validatePositiveId(id);
        validateActor(actor);

        return changeState(id, RequestState.VERIFIED, null, actor);
    }

    /**
     * Moves VERIFIED -> ACCEPTED.
     */
    @Transactional
    @CacheEvict(cacheNames = "requests", key = "#id")
    @RateLimiter(name = "requestWrite")
    @Bulkhead(name = "requestWrite", type = Bulkhead.Type.SEMAPHORE)
    public RequestResponse accept(long id, String actor) {

        validatePositiveId(id);
        validateActor(actor);

        return changeState(id, RequestState.ACCEPTED, null, actor);
    }

    /**
     * Moves VERIFIED/ACCEPTED -> REJECTED.
     */
    @Transactional
    @CacheEvict(cacheNames = "requests", key = "#id")
    @RateLimiter(name = "requestWrite")
    @Bulkhead(name = "requestWrite", type = Bulkhead.Type.SEMAPHORE)
    public RequestResponse reject(long id, String reason, String actor) {

        validatePositiveId(id);
        validateActor(actor);

        String normalizedReason = normalizeRequired(reason, "A reason is required when rejecting a request");

        return changeState(id, RequestState.REJECTED, normalizedReason, actor);
    }

    /**
     * Publishes an ACCEPTED request.
     * <p>
     * Publication number generation contains its own
     * database retry logic.
     */
    @Transactional
    @CacheEvict(cacheNames = "requests", key = "#id")
    @RateLimiter(name = "requestWrite")
    @Bulkhead(name = "requestWrite", type = Bulkhead.Type.SEMAPHORE)
    public RequestResponse publish(long id, String actor) {

        validatePositiveId(id);
        validateActor(actor);

        RequestEntity entity = getEntity(id);

        if (entity.getState() != RequestState.ACCEPTED) {

            throw new BusinessRuleViolationException("Only an ACCEPTED request can be published");
        }

        RequestState currentState = entity.getState();

        stateMachine.validate(currentState, RequestState.PUBLISHED);

        /*
         * PublicationNumberGenerator performs:
         *
         * 1 initial attempt
         * +
         * 3 retries
         * +
         * 1 second delay between attempts
         *
         * If all attempts fail, it throws
         * DatabaseRetryException.
         */
        Long publicationNumber = publicationNumberGenerator.next();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        entity.changeState(RequestState.PUBLISHED, null, publicationNumber, now);

        auditRepository.save(RequestAuditEntity.stateChanged(entity.getId(), currentState, RequestState.PUBLISHED, null, actor, now));

        return RequestResponse.from(entity);
    }

    /**
     * Logically deletes a request.
     * <p>
     * Deletion is allowed only from CREATED state.
     */
    @Transactional
    @CacheEvict(cacheNames = "requests", key = "#id")
    @RateLimiter(name = "requestWrite")
    @Bulkhead(name = "requestWrite", type = Bulkhead.Type.SEMAPHORE)
    public RequestResponse delete(long id, String reason, String actor) {

        validatePositiveId(id);
        validateActor(actor);

        String normalizedReason = normalizeRequired(reason, "A reason is required when deleting a request");

        RequestEntity entity = getEntity(id);

        if (entity.getState() != RequestState.CREATED) {

            throw new BusinessRuleViolationException("A request can only be deleted while it is CREATED");
        }

        return changeState(entity, RequestState.DELETED, normalizedReason, actor);
    }

    /**
     * Returns complete audit history.
     */
    @Transactional(readOnly = true)
    @RateLimiter(name = "requestRead")
    @Bulkhead(name = "requestRead", type = Bulkhead.Type.SEMAPHORE)
    public List<AuditResponse> audit(long id) {

        validatePositiveId(id);

        /*
         * Also validates that the request exists.
         */
        getEntity(id);

        return auditRepository.findByRequestIdOrderByOccurredAtAscIdAsc(id).stream().map(AuditResponse::from).toList();
    }

    /**
     * Changes request state after validating
     * the transition using the state machine.
     */
    private RequestResponse changeState(long id, RequestState targetState, String reason, String actor) {

        return changeState(getEntity(id), targetState, reason, actor);
    }

    private RequestResponse changeState(RequestEntity entity, RequestState targetState, String reason, String actor) {

        RequestState currentState = entity.getState();

        stateMachine.validate(currentState, targetState);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        entity.changeState(targetState, reason, entity.getPublicationNumber(), now);

        auditRepository.save(RequestAuditEntity.stateChanged(entity.getId(), currentState, targetState, reason, actor, now));

        return RequestResponse.from(entity);
    }

    private void validateContentEditable(RequestEntity entity) {

        RequestState state = entity.getState();

        if (state != RequestState.CREATED && state != RequestState.VERIFIED) {

            throw new BusinessRuleViolationException("Content may only be modified while request " + "is CREATED or VERIFIED");
        }
    }

    private void validateActor(String actor) {

        if (actor == null || actor.isBlank()) {

            throw new BusinessRuleViolationException("Authenticated user is required");
        }
    }

    private void validatePositiveId(long id) {

        if (id <= 0) {

            throw new BusinessRuleViolationException("Request ID must be greater than zero");
        }
    }

    private static String normalizeRequired(String value, String errorMessage) {

        if (value == null) {
            throw new BusinessRuleViolationException(errorMessage);
        }

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new BusinessRuleViolationException(errorMessage);
        }

        return normalized;
    }

    private static Pageable normalizePageable(Pageable pageable) {

        if (pageable == null || pageable.isUnpaged()) {

            return PageRequest.of(0, DEFAULT_PAGE_SIZE, defaultSort());
        }

        int pageNumber = Math.max(pageable.getPageNumber(), 0);

        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);

        return PageRequest.of(pageNumber, pageSize, normalizeSort(pageable.getSort()));
    }

    private static Sort normalizeSort(Sort requestedSort) {

        if (requestedSort == null || requestedSort.isUnsorted()) {

            return defaultSort();
        }

        List<Sort.Order> validOrders = requestedSort.stream().filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty())).toList();

        return validOrders.isEmpty() ? defaultSort() : Sort.by(validOrders);
    }

    private static Sort defaultSort() {

        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }

    private RequestEntity getEntity(long id) {

        return requestRepository.findById(id).orElseThrow(() -> new NotFoundException("Request " + id + " not found"));
    }
}
