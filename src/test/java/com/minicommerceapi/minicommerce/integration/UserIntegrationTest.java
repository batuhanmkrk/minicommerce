package com.minicommerceapi.minicommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerceapi.minicommerce.dto.UserDtos;
import com.minicommerceapi.minicommerce.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
@Transactional
class UserIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser_Success() throws Exception {
        UserDtos.CreateUserRequest request = new UserDtos.CreateUserRequest(
                "Alice Johnson",
                "alice@example.com"
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void testGetUser_Success() throws Exception {
        // Create user first
        UserDtos.CreateUserRequest createReq = new UserDtos.CreateUserRequest(
                "Bob Smith",
                "bob@example.com"
        );

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserDtos.UserResponse createdUser = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                UserDtos.UserResponse.class
        );

        // GET the user
        mockMvc.perform(get("/api/users/" + createdUser.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdUser.id()))
                .andExpect(jsonPath("$.name").value("Bob Smith"))
                .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    @Test
    void testGetUser_NotFound() throws Exception {
        mockMvc.perform(get("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListUsers_Success() throws Exception {
        // Create multiple users
        UserDtos.CreateUserRequest user1 = new UserDtos.CreateUserRequest("User1", "user1@example.com");
        UserDtos.CreateUserRequest user2 = new UserDtos.CreateUserRequest("User2", "user2@example.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated());

        // List all users
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("User1", "User2")));
    }

    @Test
    void testUpdateUser_Success_PutMethod() throws Exception {
        // Create user
        UserDtos.CreateUserRequest createReq = new UserDtos.CreateUserRequest(
                "Original Name",
                "original@example.com"
        );

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserDtos.UserResponse createdUser = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                UserDtos.UserResponse.class
        );

        // Update user (PUT)
        UserDtos.UpdateUserRequest updateReq = new UserDtos.UpdateUserRequest(
                "Updated Name",
                "updated@example.com"
        );

        mockMvc.perform(put("/api/users/" + createdUser.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdUser.id()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        // Create user
        UserDtos.CreateUserRequest createReq = new UserDtos.CreateUserRequest(
                "To Delete",
                "delete@example.com"
        );

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserDtos.UserResponse createdUser = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                UserDtos.UserResponse.class
        );

        // DELETE the user
        mockMvc.perform(delete("/api/users/" + createdUser.id()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/users/" + createdUser.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateUser_InvalidEmail_BadRequest() throws Exception {
        UserDtos.CreateUserRequest invalidReq = new UserDtos.CreateUserRequest(
                "Invalid User",
                "not-an-email"
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUser_BlankName_BadRequest() throws Exception {
        UserDtos.CreateUserRequest invalidReq = new UserDtos.CreateUserRequest(
                "",
                "valid@example.com"
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }
}
