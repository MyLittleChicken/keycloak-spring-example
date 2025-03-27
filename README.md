# Spring Boot와 Keycloak 인증 예제

Spring Boot와 Keycloak을 활용한 인증 시스템 예제 프로젝트입니다. 역할 기반 접근 제어(RBAC)를 구현하여 공개/사용자/관리자 수준의 페이지와 API를 제공합니다.

## 실행 방법

```bash
# 1. Keycloak 서버 실행
docker-compose up -d

# 2. 애플리케이션 빌드 및 실행
./gradlew bootRun
```

- Keycloak 관리자 접속: http://localhost:8180 (admin/admin)
- 애플리케이션 접속: http://localhost:8080

## 테스트 계정

- 관리자: admin/admin
- 일반 사용자: 회원가입 페이지에서 직접 생성

## 주요 URL 및 기능

### 페이지

| URL | 설명 | 접근 권한 |
|-----|------|----------|
| `/` | 홈 페이지 | 모든 사용자 |
| `/public` | 공개 페이지 | 모든 사용자 |
| `/user` | 사용자 페이지 | 인증된 사용자(user, admin) |
| `/admin` | 관리자 페이지 | 관리자(admin) |
| `/register` | 회원가입 페이지 | 모든 사용자 |
| `/login` | 로그인 페이지 | 모든 사용자 |

### API

| URL | 설명 | 접근 권한 |
|-----|------|----------|
| `/api/public` | 공개 API | 모든 사용자 |
| `/api/user` | 사용자 API | 인증된 사용자(user, admin) |
| `/api/admin` | 관리자 API | 관리자(admin) |

## 프로젝트 구조

- `src/main/java/com/example/springbootkeycloak/`
  - `config/`: 설정 클래스 (Keycloak, Security, GlobalAdvice)
  - `controller/`: 컨트롤러 (인증, 페이지)
  - `dto/`: 데이터 전송 객체
  - `service/`: 서비스 (Keycloak 연동, 인증)

## 기술 스택

- Spring Boot 3.4.4
- Spring Security
- Keycloak 24.0.1
- Thymeleaf
- Bootstrap 5

## 주요 기능

- Keycloak 기반 인증 시스템
- 프로그래밍 방식의 로그인/로그아웃
- 역할 기반 접근 제어(RBAC)
- 자동 역할 및 관리자 계정 생성

## 참고사항

- 개발 목적의 예제 프로젝트이므로 실제 프로덕션 환경에서는 추가적인 보안 설정이 필요합니다.
- Keycloak 서버는 개발 모드(start-dev)로 설정되어 있어 실제 운영 환경에서는 적절히 변경해야 합니다.
