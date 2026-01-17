package com.minicommerceapi.minicommerce;

import com.minicommerceapi.minicommerce.dto.UserDtos;
import com.minicommerceapi.minicommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a user")
    @PostMapping
    public ResponseEntity<UserDtos.UserResponse> create(@Valid @RequestBody UserDtos.CreateUserRequest req) {
        UserDtos.UserResponse created = userService.create(req);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @Operation(summary = "List users")
    @GetMapping
    public List<UserDtos.UserResponse> list() {
        return userService.list();
    }

    @Operation(summary = "Get a user by id")
    @GetMapping("/{id}")
    public UserDtos.UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @Operation(summary = "Update a user (PUT)")
    @PutMapping("/{id}")
    public UserDtos.UserResponse update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserRequest req) {
        return userService.update(id, req);
    }

    @Operation(summary = "Delete a user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
