package com.meli.productapi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageResponse — pagination wrapper tests")
class PageResponseTest {

    @Test
    @DisplayName("Should return correct metadata when paginating")
    void testOfWithPagination_ShouldReturnCorrectMetadata() {
        List<String> items = Arrays.asList("A", "B", "C", "D", "E");

        PageResponse<String> page = PageResponse.of(items, 1, 2);

        assertEquals(List.of("A", "B"), page.getContent());
        assertEquals(1, page.getPage());
        assertEquals(2, page.getPageSize());
        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertTrue(page.isHasNext());
        assertFalse(page.isHasPrevious());
    }

    @Test
    @DisplayName("Should return empty content when page is beyond total")
    void testOfWithPageBeyondTotal_ShouldReturnEmptyContent() {
        List<String> items = Arrays.asList("A", "B");

        PageResponse<String> page = PageResponse.of(items, 10, 2);

        assertTrue(page.getContent().isEmpty());
        assertEquals(10, page.getPage());
        assertEquals(2, page.getPageSize());
        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getTotalPages());
        assertFalse(page.isHasNext());
        assertTrue(page.isHasPrevious());
    }

    @Test
    @DisplayName("Should return all items with single page metadata via ofAll")
    void testOfAll_ShouldReturnAllItemsWithSinglePage() {
        List<String> items = Arrays.asList("A", "B", "C");

        PageResponse<String> page = PageResponse.ofAll(items);

        assertEquals(3, page.getContent().size());
        assertEquals(1, page.getPage());
        assertEquals(3, page.getPageSize());
        assertEquals(3, page.getTotalElements());
        assertEquals(1, page.getTotalPages());
        assertFalse(page.isHasNext());
        assertFalse(page.isHasPrevious());
    }

    @Test
    @DisplayName("hasNext should be true when more pages exist")
    void testHasNext_ShouldBeTrueWhenMorePagesExist() {
        List<String> items = Arrays.asList("A", "B", "C", "D");

        PageResponse<String> page1 = PageResponse.of(items, 1, 2);
        assertTrue(page1.isHasNext());

        PageResponse<String> page2 = PageResponse.of(items, 2, 2);
        assertFalse(page2.isHasNext());
    }

    @Test
    @DisplayName("hasPrevious should be true when not on first page")
    void testHasPrevious_ShouldBeTrueWhenNotFirstPage() {
        List<String> items = Arrays.asList("A", "B", "C", "D");

        PageResponse<String> page1 = PageResponse.of(items, 1, 2);
        assertFalse(page1.isHasPrevious());

        PageResponse<String> page2 = PageResponse.of(items, 2, 2);
        assertTrue(page2.isHasPrevious());
    }

    @Test
    @DisplayName("Should return empty PageResponse when given empty list")
    void testOfWithEmptyList_ShouldReturnEmptyPageResponse() {
        List<String> items = Collections.emptyList();

        PageResponse<String> page = PageResponse.of(items, 1, 10);

        assertTrue(page.getContent().isEmpty());
        assertEquals(1, page.getPage());
        assertEquals(10, page.getPageSize());
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getTotalPages());
        assertFalse(page.isHasNext());
        assertFalse(page.isHasPrevious());
    }

    @Test
    @DisplayName("ofAll with empty list should return empty single-page response")
    void testOfAllWithEmptyList_ShouldReturnEmptySinglePage() {
        List<String> items = Collections.emptyList();

        PageResponse<String> page = PageResponse.ofAll(items);

        assertTrue(page.getContent().isEmpty());
        assertEquals(1, page.getPage());
        assertEquals(0, page.getTotalElements());
        assertEquals(1, page.getTotalPages());
        assertFalse(page.isHasNext());
        assertFalse(page.isHasPrevious());
    }
}
