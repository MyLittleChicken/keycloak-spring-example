package com.example.springbootkeycloak.config;

import com.example.springbootkeycloak.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalControllerAdvice {

    private final AuthService authService;

    @ModelAttribute("authenticated")
    public boolean isAuthenticated(HttpSession session) {
        return authService.isAuthenticated(session);
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpSession session) {
        return authService.isAdmin(session);
    }

    @ModelAttribute("isUser")
    public boolean isUser(HttpSession session) {
        return authService.isUser(session);
    }

    @ModelAttribute("username")
    public String getUsername(HttpSession session) {
        if (authService.isAuthenticated(session)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                return auth.getName();
            } else {
                log.debug("Spring Security Authentication 정보 없음. 세션에서 정보 추출");
                // 세션에서 사용자 이름을 가져오는 대비 로직을 여기에 추가할 수 있습니다
                return "사용자";
            }
        }
        return null;
    }
}
