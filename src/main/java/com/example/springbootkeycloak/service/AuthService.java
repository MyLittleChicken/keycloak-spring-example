package com.example.springbootkeycloak.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String TOKEN_SESSION_ATTR = "keycloak_token";
    private static final String REFRESH_TOKEN_SESSION_ATTR = "keycloak_refresh_token";
    private static final String USER_ROLES_SESSION_ATTR = "user_roles";

    /**
     * 현재 세션에서 인증 여부 확인
     */
    public boolean isAuthenticated(HttpSession session) {
        boolean authenticated = session.getAttribute(TOKEN_SESSION_ATTR) != null;
        log.debug("인증 여부 확인: {}", authenticated);
        return authenticated;
    }

    /**
     * 특정 역할을 가지고 있는지 확인
     */
    public boolean hasRole(HttpSession session, String role) {
        if (!isAuthenticated(session)) {
            log.debug("인증되지 않았으므로 역할 체크 false: {}", role);
            return false;
        }
        
        String roles = (String) session.getAttribute(USER_ROLES_SESSION_ATTR);
        log.debug("세션에 저장된 역할: {}, 확인할 역할: {}", roles, role);
        
        if (roles == null) {
            log.debug("역할 정보가 없음");
            return false;
        }
        
        // 역할 목록에서 해당 역할이 있는지 체크
        boolean hasRole = roles.toLowerCase().contains(role.toLowerCase());
        log.debug("역할 '{}' 포함 여부: {}", role, hasRole);
        return hasRole;
    }

    /**
     * 관리자 권한 확인
     */
    public boolean isAdmin(HttpSession session) {
        boolean isAdmin = hasRole(session, "admin");
        log.debug("관리자 권한 확인: {}", isAdmin);
        return isAdmin;
    }

    /**
     * 일반 사용자 권한 확인
     */
    public boolean isUser(HttpSession session) {
        boolean isUser = hasRole(session, "user");
        log.debug("사용자 권한 확인: {}", isUser);
        return isUser;
    }

    /**
     * 로그인 세션 정보 저장
     */
    public void setLoginSession(HttpSession session, AccessTokenResponse tokenResponse, String roles) {
        log.debug("세션에 인증 정보 저장 - 역할: {}", roles);
        session.setAttribute(TOKEN_SESSION_ATTR, tokenResponse.getToken());
        session.setAttribute(REFRESH_TOKEN_SESSION_ATTR, tokenResponse.getRefreshToken());
        session.setAttribute(USER_ROLES_SESSION_ATTR, roles);
    }

    /**
     * 로그아웃 세션 정보 삭제
     */
    public void clearLoginSession(HttpSession session) {
        log.debug("세션에서 인증 정보 제거");
        session.removeAttribute(TOKEN_SESSION_ATTR);
        session.removeAttribute(REFRESH_TOKEN_SESSION_ATTR);
        session.removeAttribute(USER_ROLES_SESSION_ATTR);
    }
}
