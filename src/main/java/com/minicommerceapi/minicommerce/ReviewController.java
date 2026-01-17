package com.minicommerceapi.minicommerce;

import com.minicommerceapi.minicommerce.dto.ReviewDtos;
import com.minicommerceapi.minicommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Create a review")
    @PostMapping
    public ResponseEntity<ReviewDtos.ReviewResponse> create(@Valid @RequestBody ReviewDtos.CreateReviewRequest req) {
        ReviewDtos.ReviewResponse created = reviewService.create(req);
        return ResponseEntity.created(URI.create("/api/reviews/" + created.id())).body(created);
    }

    @Operation(summary = "List reviews (optional filter by productId)")
    @GetMapping
    public List<ReviewDtos.ReviewResponse> list(@RequestParam(required = false) Long productId) {
        return reviewService.list(productId);
    }

    @Operation(summary = "Get review by id")
    @GetMapping("/{id}")
    public ReviewDtos.ReviewResponse get(@PathVariable Long id) {
        return reviewService.get(id);
    }

    @Operation(summary = "Patch review (rating/comment)")
    @PatchMapping("/{id}")
    public ReviewDtos.ReviewResponse patch(@PathVariable Long id, @Valid @RequestBody ReviewDtos.PatchReviewRequest req) {
        return reviewService.patch(id, req);
    }

    @Operation(summary = "Delete review")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
