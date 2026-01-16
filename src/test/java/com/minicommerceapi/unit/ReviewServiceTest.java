package com.minicommerceapi.unit;

import com.minicommerceapi.domain.User;
import com.minicommerceapi.dto.ReviewDtos;
import com.minicommerceapi.exception.NotFoundException;
import com.minicommerceapi.repo.ProductRepository;
import com.minicommerceapi.repo.ReviewRepository;
import com.minicommerceapi.repo.UserRepository;
import com.minicommerceapi.service.ReviewService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Test
    void create_missingUser_throwsNotFound() {
        ReviewRepository rr = mock(ReviewRepository.class);
        ProductRepository pr = mock(ProductRepository.class);
        UserRepository ur = mock(UserRepository.class);

        when(ur.findById(1L)).thenReturn(Optional.empty());

        ReviewService svc = new ReviewService(rr, pr, ur);
        assertThrows(NotFoundException.class, () -> svc.create(new ReviewDtos.CreateReviewRequest(1L, 2L, 5, "ok")));
    }

    @Test
    void create_missingProduct_throwsNotFound() {
        ReviewRepository rr = mock(ReviewRepository.class);
        ProductRepository pr = mock(ProductRepository.class);
        UserRepository ur = mock(UserRepository.class);

        User u = new User(); u.setName("A"); u.setEmail("a@b.com");
        when(ur.findById(1L)).thenReturn(Optional.of(u));
        when(pr.findById(2L)).thenReturn(Optional.empty());

        ReviewService svc = new ReviewService(rr, pr, ur);
        assertThrows(NotFoundException.class, () -> svc.create(new ReviewDtos.CreateReviewRequest(1L, 2L, 5, "ok")));
    }
}
