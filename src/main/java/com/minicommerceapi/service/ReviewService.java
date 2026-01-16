package com.minicommerceapi.service;

import com.minicommerceapi.domain.Product;
import com.minicommerceapi.domain.Review;
import com.minicommerceapi.domain.User;
import com.minicommerceapi.dto.ReviewDtos;
import com.minicommerceapi.exception.NotFoundException;
import com.minicommerceapi.repo.ProductRepository;
import com.minicommerceapi.repo.ReviewRepository;
import com.minicommerceapi.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewDtos.ReviewResponse create(ReviewDtos.CreateReviewRequest req) {
        User user = userRepository.findById(req.userId()).orElseThrow(() -> new NotFoundException("User not found"));
        Product product = productRepository.findById(req.productId()).orElseThrow(() -> new NotFoundException("Product not found"));

        Review r = new Review();
        r.setUser(user);
        r.setProduct(product);
        r.setRating(req.rating());
        r.setComment(req.comment());
        r = reviewRepository.save(r);

        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.ReviewResponse> list(Long productId) {
        if (productId == null) {
            return reviewRepository.findAll().stream().map(this::toResponse).toList();
        }
        return reviewRepository.findByProductId(productId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReviewDtos.ReviewResponse get(Long id) {
        Review r = reviewRepository.findById(id).orElseThrow(() -> new NotFoundException("Review not found"));
        return toResponse(r);
    }


@Transactional
public ReviewDtos.ReviewResponse patch(Long id, ReviewDtos.PatchReviewRequest req) {
    Review r = reviewRepository.findById(id).orElseThrow(() -> new NotFoundException("Review not found"));
    if (req.rating() != null) {
        r.setRating(req.rating());
    }
    if (req.comment() != null) {
        r.setComment(req.comment());
    }
    return toResponse(r);
}

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Review not found");
        }
        reviewRepository.deleteById(id);
    }

    private ReviewDtos.ReviewResponse toResponse(Review r) {
        return new ReviewDtos.ReviewResponse(r.getId(), r.getUser().getId(), r.getProduct().getId(), r.getRating(), r.getComment());
    }
}
