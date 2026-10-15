package com.hrconsultancy.users.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hrconsultancy.users.controller.UserManagementController;
import com.hrconsultancy.users.service.UserManagementService;

@WebMvcTest(UserManagementController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementService userManagementService;

    @Test
    void unauthenticatedUserCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hrAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(user("dev.user").roles("HR_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void hrManagerCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(user("test.manager").roles("HR_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void hrManagerCannotCreateUsers() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .with(user("test.manager").roles("HR_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "test.recruiter2",
                          "email": "test.recruiter2@example.com",
                          "firstName": "Test",
                          "lastName": "Recruiter",
                          "roles": ["RECRUITER"]
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(user("test.recruiter").roles("RECRUITER")))
                .andExpect(status().isForbidden());
    }
}