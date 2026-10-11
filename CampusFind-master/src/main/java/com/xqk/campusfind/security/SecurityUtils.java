package com.xqk.campusfind.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：从已认证的 JWT（SecurityContext 中的 LoginUser）读取当前用户身份，
 * 使后端以令牌为准，忽略前端传入的用户参数，杜绝越权篡改。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户，未登录返回 null */
    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 获取当前登录用户 ID，未登录返回 null */
    public static Long getCurrentUserId() {
        LoginUser user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
}
