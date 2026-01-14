package edu.akademik.minicommerce.unit;

import edu.akademik.minicommerce.dto.UserDtos;
import edu.akademik.minicommerce.exception.ConflictException;
import edu.akademik.minicommerce.exception.NotFoundException;
import edu.akademik.minicommerce.repo.UserRepository;
import edu.akademik.minicommerce.service.UserService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Test
    void create_normalizesEmail_andPersists() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        when(repo.existsByEmail("a@b.com")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            var u = (edu.akademik.minicommerce.domain.User) inv.getArgument(0);
            Field f = edu.akademik.minicommerce.domain.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, 1L);
            return u;
        });

        UserService svc = new UserService(repo);
        var res = svc.create(new UserDtos.CreateUserRequest(" Ali ", " A@B.COM "));

        assertEquals(1L, res.id());
        assertEquals("Ali", res.name());
        assertEquals("a@b.com", res.email());
    }

    @Test
    void create_duplicateEmail_throwsConflict() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.existsByEmail("x@y.com")).thenReturn(true);

        UserService svc = new UserService(repo);
        assertThrows(ConflictException.class,
                () -> svc.create(new UserDtos.CreateUserRequest("X", "x@y.com")));
    }

    @Test
    void get_missingUser_throwsNotFound() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        UserService svc = new UserService(repo);
        assertThrows(NotFoundException.class, () -> svc.get(99L));
    }
}
