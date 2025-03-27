package com.example.springbootkeycloak.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KeycloakInitializer implements CommandLineRunner {

    private final Keycloak keycloak;
    
    @Value("${keycloak.admin.realm}")
    private String realm;

    @Override
    public void run(String... args) throws Exception {
        log.info("Keycloak 초기화 시작");
        try {
            RealmResource realmResource = keycloak.realm(realm);
            
            // 1. 'admin' 역할이 없으면 생성
            try {
                realmResource.roles().get("admin").toRepresentation();
                log.info("'admin' 역할이 이미 존재함");
            } catch (Exception e) {
                log.info("'admin' 역할 생성");
                RoleRepresentation adminRole = new RoleRepresentation();
                adminRole.setName("admin");
                adminRole.setDescription("관리자 역할");
                realmResource.roles().create(adminRole);
            }
            
            // 2. 'user' 역할이 없으면 생성
            try {
                realmResource.roles().get("user").toRepresentation();
                log.info("'user' 역할이 이미 존재함");
            } catch (Exception e) {
                log.info("'user' 역할 생성");
                RoleRepresentation userRole = new RoleRepresentation();
                userRole.setName("user");
                userRole.setDescription("일반 사용자 역할");
                realmResource.roles().create(userRole);
            }
            
            // 3. 'admin' 사용자가 없으면 생성
            List<UserRepresentation> adminUsers = realmResource.users().search("admin", true);
            if (adminUsers.isEmpty()) {
                log.info("'admin' 사용자 생성");
                UserRepresentation adminUser = new UserRepresentation();
                adminUser.setUsername("admin");
                adminUser.setEmail("admin@example.com");
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
                adminUser.setEnabled(true);
                
                // 비밀번호 설정
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue("admin");
                credential.setTemporary(false);
                adminUser.setCredentials(Collections.singletonList(credential));
                
                // 사용자 생성
                Response response = realmResource.users().create(adminUser);
                if (response.getStatus() == 201) {
                    log.info("'admin' 사용자 생성 성공");
                    String userId = extractCreatedId(response);
                    
                    // admin 역할 할당
                    UserResource userResource = realmResource.users().get(userId);
                    RoleRepresentation adminRole = realmResource.roles().get("admin").toRepresentation();
                    userResource.roles().realmLevel().add(Collections.singletonList(adminRole));
                    
                    // user 역할도 할당
                    RoleRepresentation userRole = realmResource.roles().get("user").toRepresentation();
                    userResource.roles().realmLevel().add(Collections.singletonList(userRole));
                    log.info("'admin' 사용자에게 admin 및 user 역할 할당 완료");
                } else {
                    log.warn("'admin' 사용자 생성 실패: 상태 코드 {}", response.getStatus());
                }
            } else {
                log.info("'admin' 사용자가 이미 존재함");
                // 이미 존재하는 admin 사용자에게 admin 역할 확인 및 할당
                String userId = adminUsers.get(0).getId();
                UserResource userResource = realmResource.users().get(userId);
                List<RoleRepresentation> userRoles = userResource.roles().realmLevel().listAll();
                
                boolean hasAdminRole = userRoles.stream()
                        .anyMatch(role -> role.getName().equals("admin"));
                
                if (!hasAdminRole) {
                    RoleRepresentation adminRole = realmResource.roles().get("admin").toRepresentation();
                    userResource.roles().realmLevel().add(Collections.singletonList(adminRole));
                    log.info("기존 'admin' 사용자에게 admin 역할 할당 완료");
                }
                
                boolean hasUserRole = userRoles.stream()
                        .anyMatch(role -> role.getName().equals("user"));
                
                if (!hasUserRole) {
                    RoleRepresentation userRole = realmResource.roles().get("user").toRepresentation();
                    userResource.roles().realmLevel().add(Collections.singletonList(userRole));
                    log.info("기존 'admin' 사용자에게 user 역할 할당 완료");
                }
            }
            
            log.info("Keycloak 초기화 완료");
        } catch (Exception e) {
            log.error("Keycloak 초기화 중 오류 발생", e);
        }
    }
    
    private String extractCreatedId(Response response) {
        String locationHeader = response.getHeaderString("Location");
        if (locationHeader != null) {
            return locationHeader.replaceAll(".*/([^/]+)$", "$1");
        }
        return null;
    }
}
