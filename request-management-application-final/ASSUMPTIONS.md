# Assumptions

The supplied text refers to a state diagram, but the diagram itself was not included. The implementation therefore uses the following baseline lifecycle:

CREATED
├── VERIFIED
│     ├── ACCEPTED
│     │     ├── PUBLISHED
│     │     └── REJECTED
│     └── REJECTED
└── DELETED

The transition table lives in `RequestStateMachine` and should be updated directly when the authoritative state diagram is available.

Other assumptions:

- Deletion is logical so that audit history remains available.It will mark the request as DELETED and will not allow any further state transitions.
- Reject/delete operations require a non-blank reason.
- Publication is allowed only from ACCEPTED.
- Publication numbers are allocated from Oracle `PUBLICATION_SEQ` and are unique independently of the request primary key.
- Content can be edited only in CREATED and VERIFIED.
- Audit is implemented even though optional because it is low-cost and strengthens traceability.
- Pagination defaults to 10 and is capped at 100.
- HTTP Basic is included only as a self-contained test security mechanism. Production should use corporate OAuth2/OIDC/JWT authentication.
- Credentials are externalized through environment variables.
