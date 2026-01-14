package edu.akademik.minicommerce.unit;

import edu.akademik.minicommerce.domain.Review;
import edu.akademik.minicommerce.dto.ReviewDtos;
import edu.akademik.minicommerce.repo.ProductRepository;
import edu.akademik.minicommerce.repo.ReviewRepository;
import edu.akademik.minicommerce.repo.UserRepository;
import edu.akademik.minicommerce.service.ReviewService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReviewPatchTest {

    @Test
    void patch_updatesFields() {
        ReviewRepository rr = mock(ReviewRepository.class);
        ProductRepository pr = mock(ProductRepository.class);
        UserRepository ur = mock(UserRepository.class);

        Review r = new Review();
        r.setRating(2);
        r.setComment("ok");

        when(rr.findById(1L)).thenReturn(Optional.of(r));

        ReviewService svc = new ReviewService(rr, pr, ur);
        var res = svc.patch(1L, new ReviewDtos.PatchReviewRequest(5, "great"));

        assertEquals(5, res.rating());
        assertEquals("great", res.comment());
    }
}
