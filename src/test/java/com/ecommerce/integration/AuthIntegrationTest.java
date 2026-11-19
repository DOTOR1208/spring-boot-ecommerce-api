package com.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ecommerce.dto.request.SignInRequest;
import com.ecommerce.dto.request.SignUpRequest;
import com.ecommerce.model.User;
import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("Authentication & Security Full Integration Tests (MySQL Testcontainers)")
class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Sign-up flow: should persist user in real MySQL and return 200 OK")
    void signUp_ValidRequest_PersistsUserInMySQLAndReturnsSuccess() {
        SignUpRequest request = SignUpRequest.builder()
                .name("Alice Wonder")
                .userName("alice_integration")
                .email("alice_integration@example.com")
                .password("SecurePass123!")
                .build();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("operationType").asText()).isEqualTo("SUCCESS");
        assertThat(response.getBody().get("data").get("userName").asText()).isEqualTo("alice_integration");

        // Verify entity persisted in real MySQL container
        Optional<User> persisted = userRepository.findByUserName("alice_integration");
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getEmail()).isEqualTo("alice_integration@example.com");
        assertThat(persisted.get().getCart()).isNotNull();
    }

    @Test
    @DisplayName("Sign-up duplicate: should reject duplicate username with 409 Conflict")
    void signUp_DuplicateUsername_ReturnsConflict() {
        SignUpRequest firstUser = SignUpRequest.builder()
                .name("Bob First")
                .userName("bob_duplicate")
                .email("bob_first@example.com")
                .password("Password123!")
                .build();
        restTemplate.postForEntity("/api/auth/signup", firstUser, JsonNode.class);

        SignUpRequest duplicateUser = SignUpRequest.builder()
                .name("Bob Second")
                .userName("bob_duplicate")
                .email("bob_second@example.com")
                .password("Password123!")
                .build();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/auth/signup",
                duplicateUser,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("operationType").asText()).isEqualTo("FAILURE");
        assertThat(response.getBody().get("code").asText()).isEqualTo("ALREADY_EXIST");
    }

    @Test
    @DisplayName("Sign-in flow: valid credentials return JWT token")
    void signIn_ValidCredentials_ReturnsJwtToken() {
        SignUpRequest user = SignUpRequest.builder()
                .name("Charlie Test")
                .userName("charlie_signin")
                .email("charlie_signin@example.com")
                .password("SecretPass456!")
                .build();
        restTemplate.postForEntity("/api/auth/signup", user, JsonNode.class);

        SignInRequest signInRequest = SignInRequest.builder()
                .userName("charlie_signin")
                .password("SecretPass456!")
                .build();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/auth/signin",
                signInRequest,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("operationType").asText()).isEqualTo("SUCCESS");

        String token = response.getBody().get("data").get("token").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Protected profile flow: GET /api/users/me with Bearer JWT returns current user profile")
    void getCurrentUserProfile_WithValidBearerToken_ReturnsProfile() {
        SignUpRequest user = SignUpRequest.builder()
                .name("Diana Prince")
                .userName("diana_profile")
                .email("diana_profile@example.com")
                .password("AmazonPass789!")
                .build();
        restTemplate.postForEntity("/api/auth/signup", user, JsonNode.class);

        SignInRequest signInRequest = SignInRequest.builder()
                .userName("diana_profile")
                .password("AmazonPass789!")
                .build();

        ResponseEntity<JsonNode> signInResponse = restTemplate.postForEntity(
                "/api/auth/signin",
                signInRequest,
                JsonNode.class
        );
        String jwtToken = signInResponse.getBody().get("data").get("token").asText();

        HttpHeaders headers = createAuthHeaders(jwtToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> profileResponse = restTemplate.exchange(
                "/api/users/me",
                HttpMethod.GET,
                requestEntity,
                JsonNode.class
        );

        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody()).isNotNull();
        assertThat(profileResponse.getBody().get("data").get("userName").asText()).isEqualTo("diana_profile");
        assertThat(profileResponse.getBody().get("data").get("email").asText()).isEqualTo("diana_profile@example.com");
    }

    @Test
    @DisplayName("Sign-in failure: wrong password returns 401 Unauthorized")
    void signIn_WrongPassword_ReturnsUnauthorized() {
        SignUpRequest user = SignUpRequest.builder()
                .name("Edward Kenway")
                .userName("edward_pass")
                .email("edward_pass@example.com")
                .password("RealPassword123!")
                .build();
        restTemplate.postForEntity("/api/auth/signup", user, JsonNode.class);

        SignInRequest wrongRequest = SignInRequest.builder()
                .userName("edward_pass")
                .password("WrongPassword999!")
                .build();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/auth/signin",
                wrongRequest,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("operationType").asText()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("Protected endpoint: access without token returns 401 Unauthorized")
    void getCurrentUserProfile_WithoutToken_ReturnsUnauthorized() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/users/me",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
