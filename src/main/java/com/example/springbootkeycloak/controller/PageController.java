package com.example.springbootkeycloak.controller;

import com.example.springbootkeycloak.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PageController {

    private final AuthService authService;

    /**
     * 공통 페이지 - 모든 사용자가 접근 가능
     */
    @GetMapping("/public")
    public String publicPage(HttpSession session) {
        log.debug("공개 페이지 접근 - 인증 상태: {}", authService.isAuthenticated(session));
        if (authService.isAuthenticated(session)) {
            log.debug("세션 역할 정보: {}", session.getAttribute("user_roles"));
        }
        return "public";
    }

    /**
     * 공통 페이지 API
     */
    @GetMapping("/api/public")
    @ResponseBody
    public Map<String, Object> publicApi(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "이 API는 모든 사용자가 접근할 수 있습니다.");
        response.put("timestamp", System.currentTimeMillis());
        if (authService.isAuthenticated(session)) {
            response.put("authenticated", true);
            response.put("isAdmin", authService.isAdmin(session));
            response.put("isUser", authService.isUser(session));
            response.put("sessionRoles", session.getAttribute("user_roles"));
        }
        return response;
    }

    /**
     * 인증된 사용자 페이지 - 유저 또는 어드민 권한 필요
     */
    @GetMapping("/user")
    public String userPage(HttpSession session, RedirectAttributes redirectAttributes) {
        log.debug("사용자 페이지 접근 시도 - 인증 상태: {}, 관리자: {}, 사용자: {}", 
                authService.isAuthenticated(session), authService.isAdmin(session), authService.isUser(session));
        log.debug("세션 역할 정보: {}", session.getAttribute("user_roles"));
        
        if (!authService.isAuthenticated(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요한 페이지입니다.");
            return "redirect:/login";
        }
        
        if (!authService.isUser(session) && !authService.isAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "이 페이지에 접근할 권한이 없습니다.");
            return "redirect:/";
        }
        
        return "user";
    }

    /**
     * 인증된 사용자 API - 유저 또는 어드민 권한 필요
     */
    @GetMapping("/api/user")
    @ResponseBody
    public Map<String, Object> userApi(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (!authService.isAuthenticated(session)) {
            response.put("error", "인증되지 않은 사용자입니다.");
            return response;
        }
        
        if (!authService.isUser(session) && !authService.isAdmin(session)) {
            response.put("error", "이 API에 접근할 권한이 없습니다.");
            return response;
        }
        
        response.put("message", "이 API는 인증된 사용자(유저 또는 어드민)만 접근할 수 있습니다.");
        response.put("timestamp", System.currentTimeMillis());
        response.put("isAdmin", authService.isAdmin(session));
        response.put("isUser", authService.isUser(session));
        response.put("sessionRoles", session.getAttribute("user_roles"));
        return response;
    }

    /**
     * 관리자 페이지 - 어드민 권한 필요
     */
    @GetMapping("/admin")
    public String adminPage(HttpSession session, RedirectAttributes redirectAttributes) {
        log.debug("관리자 페이지 접근 시도 - 인증 상태: {}, 관리자: {}", 
                authService.isAuthenticated(session), authService.isAdmin(session));
        log.debug("세션 역할 정보: {}", session.getAttribute("user_roles"));
        
        if (!authService.isAuthenticated(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요한 페이지입니다.");
            return "redirect:/login";
        }
        
        if (!authService.isAdmin(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "관리자 권한이 필요한 페이지입니다.");
            return "redirect:/";
        }
        
        return "admin";
    }

    /**
     * 관리자 API - 어드민 권한 필요
     */
    @GetMapping("/api/admin")
    @ResponseBody
    public Map<String, Object> adminApi(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (!authService.isAuthenticated(session)) {
            response.put("error", "인증되지 않은 사용자입니다.");
            return response;
        }
        
        if (!authService.isAdmin(session)) {
            response.put("error", "관리자 권한이 필요한 API입니다.");
            response.put("sessionRoles", session.getAttribute("user_roles"));
            return response;
        }
        
        response.put("message", "이 API는 관리자만 접근할 수 있습니다.");
        response.put("timestamp", System.currentTimeMillis());
        response.put("adminFeatures", new String[]{"사용자 관리", "설정 변경", "통계 조회"});
        response.put("sessionRoles", session.getAttribute("user_roles"));
        return response;
    }
}
