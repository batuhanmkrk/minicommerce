package com.minicommerceapi.minicommerce.service;

import com.minicommerceapi.minicommerce.domain.Product;
import com.minicommerceapi.minicommerce.domain.Review;
import com.minicommerceapi.minicommerce.domain.User;
import com.minicommerceapi.minicommerce.dto.ReviewDtos;
import com.minicommerceapi.minicommerce.exception.NotFoundException;
import com.minicommerceapi.minicommerce.repo.ProductRepository;
import com.minicommerceapi.minicommerce.repo.ReviewRepository;
import com.minicommerceapi.minicommerce.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        // Suppress try-with-resources warning for MockitoAnnotations in test lifecycle
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_shouldCreateReview_whenValidRequest() {
        ReviewDtos.CreateReviewRequest req = new ReviewDtos.CreateReviewRequest(1L, 2L, 5, "Great!");
        User user = new User(); user.setId(1L);
        Product product = new Product(); product.setId(2L);
        Review review = new Review(); review.setId(10L); review.setUser(user); review.setProduct(product); review.setRating(5); review.setComment("Great!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewDtos.ReviewResponse resp = reviewService.create(req);
        assertEquals(10L, resp.id());
        assertEquals(1L, resp.userId());
        assertEquals(2L, resp.productId());
        assertEquals(5, resp.rating());
        assertEquals("Great!", resp.comment());
    }

    @Test
    void create_shouldThrow_whenUserNotFound() {
        ReviewDtos.CreateReviewRequest req = new ReviewDtos.CreateReviewRequest(1L, 2L, 5, "Great!");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reviewService.create(req));
    }

    @Test
    void create_shouldThrow_whenProductNotFound() {
        ReviewDtos.CreateReviewRequest req = new ReviewDtos.CreateReviewRequest(1L, 2L, 5, "Great!");
        User user = new User(); user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reviewService.create(req));
    }

    @Test
    void list_shouldReturnAllReviews_whenProductIdNull() {
        User user = new User(); user.setId(1L);
        Product product = new Product(); product.setId(2L);
        Review review = new Review(); review.setId(1L); review.setUser(user); review.setProduct(product);
        when(reviewRepository.findAll()).thenReturn(List.of(review));
        List<ReviewDtos.ReviewResponse> result = reviewService.list(null);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void list_shouldReturnReviewsForProduct_whenProductIdGiven() {
        User user = new User(); user.setId(1L);
        Product product = new Product(); product.setId(2L);
        Review review = new Review(); review.setId(1L); review.setUser(user); review.setProduct(product);
        when(reviewRepository.findByProductId(2L)).thenReturn(List.of(review));
        List<ReviewDtos.ReviewResponse> result = reviewService.list(2L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void get_shouldReturnReview_whenExists() {
        User user = new User(); user.setId(1L);
        Product product = new Product(); product.setId(2L);
        Review review = new Review(); review.setId(1L); review.setUser(user); review.setProduct(product);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        ReviewDtos.ReviewResponse resp = reviewService.get(1L);
        assertEquals(1L, resp.id());
    }

    @Test
    void get_shouldThrow_whenNotFound() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reviewService.get(1L));
    }

    @Test
    void patch_shouldUpdateFields_whenValidRequest() {
        User user = new User(); user.setId(1L);
        Product product = new Product(); product.setId(2L);
        Review review = new Review(); review.setId(1L); review.setRating(3); review.setComment("ok"); review.setUser(user); review.setProduct(product);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        ReviewDtos.PatchReviewRequest req = new ReviewDtos.PatchReviewRequest(5, "updated");
        ReviewDtos.ReviewResponse resp = reviewService.patch(1L, req);
        assertEquals(5, review.getRating());
        assertEquals("updated", review.getComment());
        assertEquals(1L, resp.id());
    }

    @Test
    void patch_shouldThrow_whenNotFound() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());
        ReviewDtos.PatchReviewRequest req = new ReviewDtos.PatchReviewRequest(5, "updated");
        assertThrows(NotFoundException.class, () -> reviewService.patch(1L, req));
    }

    @Test
    void delete_shouldDelete_whenExists() {
        when(reviewRepository.existsById(1L)).thenReturn(true);
        reviewService.delete(1L);
        verify(reviewRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(reviewRepository.existsById(1L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> reviewService.delete(1L));
    }
}