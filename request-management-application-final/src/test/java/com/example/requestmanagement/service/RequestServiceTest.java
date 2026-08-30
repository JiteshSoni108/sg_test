package com.example.requestmanagement.service;

import com.example.requestmanagement.domain.RequestAuditEntity;
import com.example.requestmanagement.domain.RequestEntity;
import com.example.requestmanagement.domain.RequestState;
import com.example.requestmanagement.exception.BusinessRuleViolationException;
import com.example.requestmanagement.repository.RequestAuditRepository;
import com.example.requestmanagement.repository.RequestRepository;
import com.example.requestmanagement.state.RequestStateMachine;
import com.example.requestmanagement.web.CreateRequest;
import com.example.requestmanagement.web.RequestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestAuditRepository auditRepository;

    @Mock
    private RequestStateMachine stateMachine;

    @Mock
    private PublicationNumberGenerator publicationNumberGenerator;

    private RequestService requestService;


    @BeforeEach
    void setUp() {

        requestService = new RequestService(requestRepository, auditRepository, stateMachine, publicationNumberGenerator);
    }


    @Test
    void create_shouldCreateRequestInCreatedState() {

        CreateRequest request = new CreateRequest("Test Request", "Test Content");

        when(requestRepository.save(any(RequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponse response = requestService.create(request, "test-user");

        assertEquals("Test Request", response.name());

        assertEquals("Test Content", response.content());

        assertEquals(RequestState.CREATED, response.state());

        verify(requestRepository).save(any(RequestEntity.class));

        verify(auditRepository).save(any(RequestAuditEntity.class));
    }


    @Test
    void create_shouldRejectBlankName() {

        CreateRequest request = new CreateRequest("", "Test Content");

        assertThrows(BusinessRuleViolationException.class, () -> requestService.create(request, "test-user"));

        verify(requestRepository, never()).save(any());
    }


    @Test
    void create_shouldRejectBlankContent() {

        CreateRequest request = new CreateRequest("Test Request", "");

        assertThrows(BusinessRuleViolationException.class, () -> requestService.create(request, "test-user"));

        verify(requestRepository, never()).save(any());
    }


    @Test
    void create_shouldRejectMissingActor() {

        CreateRequest request = new CreateRequest("Test Request", "Test Content");

        assertThrows(BusinessRuleViolationException.class, () -> requestService.create(request, ""));

        verify(requestRepository, never()).save(any());
    }


    @Test
    void verify_shouldMoveCreatedToVerified() {

        RequestEntity entity = createEntity(1L, RequestState.CREATED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        RequestResponse response = requestService.verify(1L, "test-user");

        assertEquals(RequestState.VERIFIED, response.state());

        verify(stateMachine).validate(RequestState.CREATED, RequestState.VERIFIED);

        verify(auditRepository).save(any(RequestAuditEntity.class));
    }


    @Test
    void accept_shouldMoveVerifiedToAccepted() {

        RequestEntity entity = createEntity(1L, RequestState.VERIFIED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        RequestResponse response = requestService.accept(1L, "test-user");

        assertEquals(RequestState.ACCEPTED, response.state());

        verify(stateMachine).validate(RequestState.VERIFIED, RequestState.ACCEPTED);

        verify(auditRepository).save(any(RequestAuditEntity.class));
    }


    @Test
    void reject_shouldRejectVerifiedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.VERIFIED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        RequestResponse response = requestService.reject(1L, "Rejected by reviewer", "test-user");

        assertEquals(RequestState.REJECTED, response.state());

        verify(stateMachine).validate(RequestState.VERIFIED, RequestState.REJECTED);
    }


    @Test
    void reject_shouldRejectAcceptedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.ACCEPTED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        RequestResponse response = requestService.reject(1L, "Rejected after acceptance", "test-user");

        assertEquals(RequestState.REJECTED, response.state());

        verify(stateMachine).validate(RequestState.ACCEPTED, RequestState.REJECTED);
    }


    @Test
    void reject_shouldRejectBlankReason() {
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> requestService.reject(1L, " ", "test-user"));
        assertEquals("A reason is required when rejecting a request", exception.getMessage());
        verifyNoInteractions(requestRepository);
        verifyNoInteractions(auditRepository);
        verifyNoInteractions(stateMachine);
    }


    @Test
    void delete_shouldDeleteCreatedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.CREATED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        RequestResponse response = requestService.delete(1L, "Duplicate request", "test-user");

        assertEquals(RequestState.DELETED, response.state());

        verify(stateMachine).validate(RequestState.CREATED, RequestState.DELETED);
    }


    @Test
    void delete_shouldRejectVerifiedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.VERIFIED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(BusinessRuleViolationException.class, () -> requestService.delete(1L, "Should not be deleted", "test-user"));

        verify(stateMachine, never()).validate(RequestState.VERIFIED, RequestState.DELETED);
    }


    @Test
    void delete_shouldRejectAcceptedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.ACCEPTED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(BusinessRuleViolationException.class, () -> requestService.delete(1L, "Should not be deleted", "test-user"));
    }


    @Test
    void updateContent_shouldAllowCreatedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.CREATED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        RequestResponse response = requestService.updateContent(1L, "Updated content", "test-user");

        assertEquals("Updated content", response.content());

        verify(auditRepository).save(any(RequestAuditEntity.class));
    }


    @Test
    void updateContent_shouldAllowVerifiedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.VERIFIED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        RequestResponse response = requestService.updateContent(1L, "Updated content", "test-user");

        assertEquals("Updated content", response.content());
    }


    @Test
    void updateContent_shouldRejectAcceptedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.ACCEPTED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(BusinessRuleViolationException.class, () -> requestService.updateContent(1L, "Updated content", "test-user"));
    }


    @Test
    void publish_shouldPublishAcceptedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.ACCEPTED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        when(publicationNumberGenerator.next()).thenReturn(100001L);

        RequestResponse response = requestService.publish(1L, "test-user");

        assertEquals(RequestState.PUBLISHED, response.state());

        assertEquals(100001L, response.publicationNumber());

        verify(publicationNumberGenerator).next();

        verify(stateMachine).validate(RequestState.ACCEPTED, RequestState.PUBLISHED);

        verify(auditRepository).save(any(RequestAuditEntity.class));
    }


    @Test
    void publish_shouldRejectCreatedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.CREATED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(BusinessRuleViolationException.class, () -> requestService.publish(1L, "test-user"));

        verify(publicationNumberGenerator, never()).next();
    }


    @Test
    void publish_shouldRejectVerifiedRequest() {

        RequestEntity entity = createEntity(1L, RequestState.VERIFIED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(BusinessRuleViolationException.class, () -> requestService.publish(1L, "test-user"));

        verify(publicationNumberGenerator, never()).next();
    }


    @Test
    void verify_shouldRejectInvalidTransition() {

        RequestEntity entity = createEntity(1L, RequestState.ACCEPTED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        org.mockito.Mockito.doThrow(new BusinessRuleViolationException("Invalid state transition")).when(stateMachine).validate(RequestState.ACCEPTED, RequestState.VERIFIED);

        assertThrows(BusinessRuleViolationException.class, () -> requestService.verify(1L, "test-user"));
    }


    @Test
    void get_shouldReturnRequest() {

        RequestEntity entity = createEntity(1L, RequestState.CREATED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(entity));

        RequestResponse response = requestService.get(1L);

        assertEquals(1L, response.id());

        assertEquals(RequestState.CREATED, response.state());

        verify(requestRepository).findById(1L);
    }


    private RequestEntity createEntity(Long id, RequestState state) {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        RequestEntity entity = new RequestEntity("Test Request", "Test Content", now);

        /*
         * The test assumes RequestEntity exposes setters or
         * test-friendly state/id handling through its existing
         * implementation.
         *
         * If your entity does not expose these methods, use the
         * constructor/factory already present in your entity.
         */

        setEntityId(entity, id);
        setEntityState(entity, state);

        return entity;
    }


    private void setEntityId(RequestEntity entity, Long id) {

        try {

            var field = RequestEntity.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException exception) {

            throw new IllegalStateException("Unable to set RequestEntity id for test", exception);
        }
    }


    private void setEntityState(RequestEntity entity, RequestState state) {

        try {

            var field = RequestEntity.class.getDeclaredField("state");

            field.setAccessible(true);
            field.set(entity, state);

        } catch (ReflectiveOperationException exception) {

            throw new IllegalStateException("Unable to set RequestEntity state for test", exception);
        }
    }
}
