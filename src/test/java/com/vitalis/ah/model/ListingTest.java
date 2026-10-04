package com.vitalis.ah.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListingTest {

    private Listing listing(long expiresAt) {
        return new Listing(UUID.randomUUID(), UUID.randomUUID(), "Seller", 100.0,
                0L, expiresAt, "c29tZQ==", "STONE", "Stone", false);
    }

    @Test
    void expiryBoundary() {
        Listing l = listing(1000L);
        assertFalse(l.expired(999L));
        assertTrue(l.expired(1000L), "listing dies exactly at expiresAt");
        assertTrue(l.expired(1001L));
    }

    @Test
    void sellerFieldsSurviveSerializationRoundTripShape() {
        Listing l = listing(Long.MAX_VALUE);
        assertEquals(100.0, l.price);
        assertEquals("Seller", l.sellerName);
        assertFalse(l.expired(Long.MAX_VALUE - 1));
    }
}
