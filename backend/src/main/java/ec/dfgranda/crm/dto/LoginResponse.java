package ec.dfgranda.crm.dto;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {
}
