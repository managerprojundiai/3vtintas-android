package br.com.tresvtintas.mobile.core.network.dto;

public final class ManagedAccountRevocationDtos {
    private static final String DEVICE = "device";
    private static final String SESSION = "session";
    private static final int MINIMUM_CREDENTIAL_LENGTH = 80;
    private static final int MINIMUM_IDENTIFIER = 1;

    private ManagedAccountRevocationDtos() {
        throw new AssertionError("No instances.");
    }

    public static PrepareRequest prepare(int expectedRevision) {
        return new PrepareRequest(expectedRevision);
    }

    public record PrepareRequest(int expectedRevision) {
        public PrepareRequest {
            if (expectedRevision < MINIMUM_IDENTIFIER) {
                throw new IllegalArgumentException(
                        "Managed revocation revision is invalid.");
            }
        }
    }

    public record Target(long id, String name) {
        public Target {
            if (id < MINIMUM_IDENTIFIER) {
                throw new IllegalArgumentException(
                        "Managed revocation target ID is invalid.");
            }
            name = DtoValidation.requireText(
                    name,
                    "Managed revocation target name",
                    200);
        }
    }

    public record Resource(
            String id,
            String label,
            int revision) {
        public Resource {
            id = DtoValidation.requireUuid(
                    id,
                    "Managed revocation resource ID");
            label = DtoValidation.requireText(
                    label,
                    "Managed revocation resource label",
                    240);
            if (revision < MINIMUM_IDENTIFIER) {
                throw new IllegalArgumentException(
                        "Managed revocation resource revision is invalid.");
            }
        }
    }

    public record PreparedAction(
            String actionId,
            String kind,
            Target target,
            Resource resource,
            String consequence,
            String expiresAt) {
        public PreparedAction {
            actionId = DtoValidation.requireUuid(
                    actionId,
                    "Managed revocation action ID");
            kind = requireKind(kind);
            if (target == null || resource == null) {
                throw new IllegalArgumentException(
                        "Managed revocation preview is incomplete.");
            }
            consequence = DtoValidation.requireText(
                    consequence,
                    "Managed revocation consequence",
                    240);
            expiresAt = DtoValidation.requireInstant(
                    expiresAt,
                    "Managed revocation action expiry");
        }
    }

    public record ChallengeRequest(String confirmation) {
        private static final String CONFIRMATION =
                "REQUEST_SECURITY_ADMIN_STEP_UP";

        public ChallengeRequest {
            if (!CONFIRMATION.equals(confirmation)) {
                throw new IllegalArgumentException(
                        "Managed revocation challenge confirmation is invalid.");
            }
        }

        public static ChallengeRequest confirmed() {
            return new ChallengeRequest(CONFIRMATION);
        }
    }

    public record ChallengeResponse(
            String challengeId,
            String nonce,
            String googleServerClientId,
            String expiresAt) {
        public ChallengeResponse {
            challengeId = DtoValidation.requireUuid(
                    challengeId,
                    "Managed revocation challenge ID");
            if (nonce == null
                    || !DtoValidation.NONCE.matcher(nonce).matches()) {
                throw new IllegalArgumentException(
                        "Managed revocation nonce is invalid.");
            }
            googleServerClientId = DtoValidation.requireText(
                    googleServerClientId,
                    "Managed revocation Google client ID",
                    255);
            expiresAt = DtoValidation.requireInstant(
                    expiresAt,
                    "Managed revocation challenge expiry");
        }
    }

    public record VerifyRequest(
            String challengeId,
            String credential) {
        public VerifyRequest {
            challengeId = DtoValidation.requireUuid(
                    challengeId,
                    "Managed revocation challenge ID");
            credential = DtoValidation.requireText(
                    credential,
                    "Managed revocation credential",
                    8_192);
            if (credential.length() < MINIMUM_CREDENTIAL_LENGTH) {
                throw new IllegalArgumentException(
                        "Managed revocation credential is invalid.");
            }
        }
    }

    public record GrantResponse(
            String stepUpToken,
            String expiresAt) {
        public GrantResponse {
            if (stepUpToken == null
                    || !stepUpToken.matches(
                            "^3vsu1_[A-Za-z0-9_-]{43}$")) {
                throw new IllegalArgumentException(
                        "Managed revocation grant is invalid.");
            }
            expiresAt = DtoValidation.requireInstant(
                    expiresAt,
                    "Managed revocation grant expiry");
        }
    }

    public record ExecuteRequest(
            String confirmation,
            String stepUpToken) {
        private static final String CONFIRMATION =
                "REVOKE_MANAGED_USER_ACCESS";

        public ExecuteRequest {
            if (!CONFIRMATION.equals(confirmation)
                    || stepUpToken == null
                    || !stepUpToken.matches(
                            "^3vsu1_[A-Za-z0-9_-]{43}$")) {
                throw new IllegalArgumentException(
                        "Managed revocation execution is invalid.");
            }
        }

        public static ExecuteRequest confirmed(String stepUpToken) {
            return new ExecuteRequest(CONFIRMATION, stepUpToken);
        }
    }

    public record Result(
            String actionId,
            String kind,
            long targetUserId,
            String resourceId,
            boolean changed,
            int revision) {
        public Result {
            actionId = DtoValidation.requireUuid(
                    actionId,
                    "Managed revocation result action ID");
            kind = requireKind(kind);
            resourceId = DtoValidation.requireUuid(
                    resourceId,
                    "Managed revocation result resource ID");
            if (targetUserId < MINIMUM_IDENTIFIER
                    || !changed
                    || revision < MINIMUM_IDENTIFIER) {
                throw new IllegalArgumentException(
                        "Managed revocation result is invalid.");
            }
        }
    }

    private static String requireKind(String value) {
        if (!DEVICE.equals(value) && !SESSION.equals(value)) {
            throw new IllegalArgumentException(
                    "Managed revocation resource kind is invalid.");
        }
        return value;
    }
}
