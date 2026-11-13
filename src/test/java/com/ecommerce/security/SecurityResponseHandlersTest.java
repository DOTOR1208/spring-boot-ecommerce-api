package com.ecommerce.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServletResponse;

class SecurityResponseHandlersTest {

    private ObjectMapper objectMapper;
    private CustomAccessDeniedHandler accessDeniedHandler;
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        this.accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);
        this.authenticationEntryPoint = new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Test
    @DisplayName("CustomAccessDeniedHandler - Should return HTTP 403 Forbidden with standard ErrorResponse JSON")
    void handleAccessDenied_Returns403WithJsonEnvelope() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        accessDeniedHandler.handle(request, response, exception);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentType().contains(MediaType.APPLICATION_JSON_VALUE));

        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertNotNull(jsonNode);
        assertEquals("FAILURE", jsonNode.get("operationType").asText());
        assertEquals("FORBIDDEN", jsonNode.get("code").asText());
        assertEquals("auth", jsonNode.get("domain").asText());
        assertTrue(jsonNode.get("message").asText().contains("Access denied"));
    }

    @Test
    @DisplayName("CustomAuthenticationEntryPoint - Should return HTTP 401 Unauthorized with standard ErrorResponse JSON")
    void commenceAuthentication_Returns401WithJsonEnvelope() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        authenticationEntryPoint.commence(request, response, exception);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentType().contains(MediaType.APPLICATION_JSON_VALUE));

        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertNotNull(jsonNode);
        assertEquals("FAILURE", jsonNode.get("operationType").asText());
        assertEquals("UNAUTHORIZED", jsonNode.get("code").asText());
        assertEquals("auth", jsonNode.get("domain").asText());
        assertTrue(jsonNode.get("message").asText().contains("Authentication required"));
    }
}
