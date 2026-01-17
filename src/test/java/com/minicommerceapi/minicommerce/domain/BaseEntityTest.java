package com.minicommerceapi.minicommerce.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    static class DummyEntity extends BaseEntity { }

    @Test
    void createdAt_shouldBeInitialized() {
        DummyEntity e = new DummyEntity();
        assertNotNull(e.getCreatedAt());

        // createdAt gelecekte olmamalı (küçük tolerans)
        assertTrue(!e.getCreatedAt().isAfter(Instant.now().plusSeconds(2)));
    }

    @Test
    void setId_shouldSetId() {
        DummyEntity e = new DummyEntity();
        e.setId(42L);
        assertEquals(42L, e.getId());
    }
}
