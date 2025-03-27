package com.example.springbootkeycloak.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final Keycloak keycloak;
    private final RestTemplate restTemplate;
    
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;
    
    @Value("${keycloak.admin.realm}")
    private String realm;
    
    @Value("${keycloak.resource}")
    private String clientId;
    
    @Value("${keycloak.credentials.secret:}")
    private String clientSecret;

    /**
     * 사용자 등록
     */
    public boolean registerUser(String username, String email, String firstName, String lastName, String password) {
        CredentialRepresentation credential = createPasswordCredentials(password);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setCredentials(Collections.singletonList(credential));
        user.setEnabled(true);
        
        // 사용자 속성 설정
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("locale", Collections.singletonList("ko"));
        user.setAttributes(attributes);

        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        Response response = usersResource.create(user);
        int status = response.getStatus();
        
        // 사용자 생성 성공 시 'user' 역할 추가
        if (status == 201) {
            String userId = extractCreatedId(response);
            log.info("사용자 생성 성공. ID: {}", userId);
            
            if (userId != null) {
                try {
                    // 'user' 역할 찾기
                    RoleRepresentation userRole = realmResource.roles().get("user").toRepresentation();
                    // 사용자에게 역할 할당
                    realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(userRole));
                    log.info("사용자에게 'user' 역할 할당 완료");
                } catch (Exception e) {
                    log.error("역할 할당 중 오류 발생", e);
                }
            }
        }
        
        return status == 201; // 201 Created
    }

    private String extractCreatedId(Response response) {
        String locationHeader = response.getHeaderString("Location");
        if (locationHeader != null) {
            return locationHeader.replaceAll(".*/([^/]+)$", "$1");
        }
        return null;
    }

    private CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        return credential;
    }
    
    /**
     * 사용자 조회 (이메일로)
     */
    public List<UserRepresentation> getUserByEmail(String email) {
        return keycloak.realm(realm).users().searchByEmail(email, true);
    }
    
    /**
     * 사용자 조회 (아이디로)
     */
    public List<UserRepresentation> getUserByUsername(String username) {
        return keycloak.realm(realm).users().search(username);
    }
    
    /**
     * 사용자 역할 조회
     */
    public List<String> getUserRoles(String username) {
        try {
            // 사용자 검색
            List<UserRepresentation> users = getUserByUsername(username);
            if (users == null || users.isEmpty()) {
                log.warn("사용자를 찾을 수 없음: {}", username);
                return Collections.emptyList();
            }
            
            String userId = users.get(0).getId();
            log.debug("사용자 ID: {}", userId);
            
            // 사용자의 realm 역할 가져오기
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            List<RoleRepresentation> roles = userResource.roles().realmLevel().listEffective();
            
            List<String> roleNames = roles.stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());
            
            log.debug("사용자 역할: {}", roleNames);
            return roleNames;
        } catch (Exception e) {
            log.error("사용자 역할 조회 중 오류 발생", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 프로그래밍 방식으로 로그인
     */
    public AccessTokenResponse login(String username, String password) {
        try {
            // Keycloak 클라이언트 빌더를 사용하여 직접 인증
            Keycloak keycloakClient = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .grantType(OAuth2Constants.PASSWORD)
                .build();
                
            // 토큰 발급 요청
            AccessTokenResponse tokenResponse = keycloakClient.tokenManager().getAccessToken();
            return tokenResponse;
        } catch (Exception e) {
            log.error("Keycloak 로그인 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * 프로그래밍 방식으로 로그아웃
     */
    public boolean logout(String refreshToken) {
        try {
            // 로그아웃 요청을 위한 URL 생성
            String logoutUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
            
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // 요청 본문 설정
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", clientId);
            map.add("refresh_token", refreshToken);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            
            // 로그아웃 요청 전송
            restTemplate.postForEntity(logoutUrl, request, Void.class);
            
            return true;
        } catch (Exception e) {
            log.error("Keycloak 로그아웃 중 오류 발생", e);
            return false;
        }
    }
}
