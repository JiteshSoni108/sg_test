package com.example.requestmanagement.controller;

import com.example.requestmanagement.domain.RequestState;
import com.example.requestmanagement.service.RequestService;
import com.example.requestmanagement.web.AuditResponse;
import com.example.requestmanagement.web.CreateRequest;
import com.example.requestmanagement.web.RejectRequest;
import com.example.requestmanagement.web.RequestResponse;
import com.example.requestmanagement.web.UpdateContentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Request Management", description = "APIs for managing the lifecycle of requests")
@SecurityRequirement(name = "basicAuth")
public class RequestController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @Operation(summary = "Create request", description = "Creates a new request in CREATED state.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Request created successfully"), @ApiResponse(responseCode = "400", description = "Invalid request"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody CreateRequest request, Principal principal) {

        RequestResponse created = requestService.create(request, getActor(principal));

        return ResponseEntity.created(URI.create("/api/v1/requests/" + created.id())).body(created);
    }


    @GetMapping
    @Operation(summary = "List requests", description = """
            Returns a paginated list of requests.

            Optional filters:
            - name
            - state

            Default page size is 10.
            Maximum page size is 100.
            """)
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Requests returned successfully"), @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public Page<RequestResponse> search(@Parameter(description = "Case-insensitive request name filter", example = "customer") @RequestParam(required = false) String name,

                                        @Parameter(description = "Request state filter", example = "VERIFIED") @RequestParam(required = false) RequestState state,

                                        @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {

        return requestService.search(name, state, pageable);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get request", description = "Returns a request by ID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Request found"), @ApiResponse(responseCode = "400", description = "Invalid request ID"), @ApiResponse(responseCode = "404", description = "Request not found"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public RequestResponse get(@Parameter(description = "Request ID", example = "1", required = true) @PathVariable @Positive long id) {

        return requestService.get(id);
    }


    @PatchMapping("/{id}/content")
    @Operation(summary = "Update request content", description = """
            Updates request content.

            Content can only be modified while the request
            is in CREATED or VERIFIED state.
            """)
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Content updated successfully"), @ApiResponse(responseCode = "400", description = "Content cannot be modified in the current state"), @ApiResponse(responseCode = "404", description = "Request not found"), @ApiResponse(responseCode = "409", description = "Concurrent modification detected"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public RequestResponse updateContent(@PathVariable @Positive long id,

                                         @Valid @RequestBody UpdateContentRequest request,

                                         Principal principal) {

        return requestService.updateContent(id, request.content(), getActor(principal));
    }


    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify request", description = "Moves a CREATED request to VERIFIED state.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Request verified successfully"), @ApiResponse(responseCode = "400", description = "Request cannot be verified from its current state"), @ApiResponse(responseCode = "404", description = "Request not found"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public RequestResponse verify(@PathVariable @Positive long id,

                                  Principal principal) {

        return requestService.verify(id, getActor(principal));
    }


    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept request", description = "Moves a VERIFIED request to ACCEPTED state.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Request accepted successfully"), @ApiResponse(responseCode = "400", description = "Request cannot be accepted from its current state"), @ApiResponse(responseCode = "404", description = "Request not found"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public RequestResponse accept(@PathVariable @Positive long id,

                                  Principal principal) {

        return requestService.accept(id, getActor(principal));
    }


    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish request", description = """
            Publishes an ACCEPTED request.

            A unique numeric publication number is
            assigned during publication.
            """)
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Request published successfully"), @ApiResponse(responseCode = "400", description = "Only an ACCEPTED request can be published"), @ApiResponse(responseCode = "404", description = "Request not found"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public RequestResponse publish(@PathVariable @Positive long id,

                                   Principal principal) {

        return requestService.publish(id, getActor(principal));
    }


    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject request", description = """
            Rejects a request.

            Rejection is allowed from VERIFIED and ACCEPTED.
            A rejection reason is mandatory.
            """)
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Request rejected successfully"), @ApiResponse(responseCode = "400", description = "Missing reason or invalid state transition"), @ApiResponse(responseCode = "404", description = "Request not found"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public RequestResponse reject(@PathVariable @Positive long id,

                                  @Valid @RequestBody RejectRequest request,

                                  Principal principal) {

        return requestService.reject(id, request.reason(), getActor(principal));
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete request", description = """
            Logically deletes a request.

            The database record is retained and the request
            is moved to DELETED state.

            Deletion is allowed only while the request
            is in CREATED state.

            A deletion reason is mandatory.
            """)
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Request logically deleted"), @ApiResponse(responseCode = "400", description = "Missing reason or request cannot be deleted from its current state"), @ApiResponse(responseCode = "404", description = "Request not found"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public RequestResponse delete(@PathVariable @Positive long id,

                                  @Parameter(description = "Reason for logical deletion", example = "Duplicate request", required = true) @RequestParam String reason,

                                  Principal principal) {

        return requestService.delete(id, reason, getActor(principal));
    }


    @GetMapping("/{id}/audit")
    @Operation(summary = "Get audit history", description = "Returns the complete audit history of a request.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Audit history returned successfully"), @ApiResponse(responseCode = "404", description = "Request not found"), @ApiResponse(responseCode = "429", description = "Rate limit exceeded or service is at capacity")})
    public List<AuditResponse> audit(@PathVariable @Positive long id) {

        return requestService.audit(id);
    }


    private String getActor(Principal principal) {

        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {

            throw new IllegalStateException("Authenticated user is required");
        }

        return principal.getName();
    }
}