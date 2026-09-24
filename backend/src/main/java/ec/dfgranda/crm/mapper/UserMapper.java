package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.dto.UserResponse;
import ec.dfgranda.crm.dto.UserSummary;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.isActive(),
                user.getLastLoginAt());
    }

    public static UserSummary toSummary(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getFullName());
    }
}
