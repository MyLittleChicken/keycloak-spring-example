package com.example.springbootkeycloak.controller;

import com.example.springbootkeycloak.dto.UserDTO;
import com.example.springbootkeycloak.service.AuthService;
import com.example.springbootkeycloak.service.KeycloakService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakService keycloakService;
    private final AuthService authService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // GlobalControllerAdvice에서 인증 정보를 제공하므로 여기서는 추가할 필요 없음
        return "index";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new UserDTO());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") UserDTO userDTO, RedirectAttributes redirectAttributes) {
        try {
            boolean registered = keycloakService.registerUser(
                    userDTO.getUsername(),
                    userDTO.getEmail(),
                    userDTO.getFirstName(),
                    userDTO.getLastName(),
                    userDTO.getPassword()
            );
            
            if (registered) {
                redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
                return "redirect:/login";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "회원가입 중 오류가 발생했습니다.");
                return "redirect:/register";
            }
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "회원가입 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
    
    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username, 
                             @RequestParam("password") String password,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            AccessTokenResponse tokenResponse = keycloakService.login(username, password);
            
            if (tokenResponse != null) {
                // 사용자 역할 직접 조회
                List<String> roles = keycloakService.getUserRoles(username);
                log.info("사용자 역할: {}", roles);
                
                // 세션에 인증 정보 저장
                String rolesStr = String.join(",", roles);
                authService.setLoginSession(session, tokenResponse, rolesStr);
                
                return "redirect:/";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.");
                return "redirect:/login";
            }
        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/login";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            String refreshToken = (String) session.getAttribute("keycloak_refresh_token");
            
            if (refreshToken != null) {
                // Keycloak 로그아웃 처리
                keycloakService.logout(refreshToken);
            }
            
            // 세션에서 인증 정보 제거
            authService.clearLoginSession(session);
            
            redirectAttributes.addFlashAttribute("successMessage", "로그아웃 되었습니다.");
            return "redirect:/";
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "로그아웃 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/";
        }
    }
}
