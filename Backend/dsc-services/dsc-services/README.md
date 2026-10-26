# DSC service deployment requirements

PDF signing sessions are written to the path configured by `DSC_PDF_SIG_SHARED_PATH`.
Set this environment variable to the same mount path in every DSC replica and mount
the same writable, shared (RWX) volume there. A pod-local directory or per-pod volume
does not support requests that move between replicas.

The service intentionally has no default for `DSC_PDF_SIG_SHARED_PATH`; it must be
configured. The application creates the directory if necessary and restricts it to
the service account. The signing-session API returns an opaque `sigId`; callers must
send that ID to the follow-up PDF-sign operation instead of `tempFilePath`.

Signing session IDs are bound to the request user, tenant, and channel. Each session
is deleted after the follow-up operation and abandoned sessions older than 24 hours
are purged at service startup.
