package com.meli.productapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic wrapper for paginated API responses.
 * Provides pagination metadata alongside the actual content.
 *
 * @param <T> the type of elements in the content list
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response wrapper with metadata")
public class PageResponse<T> {

    @Schema(description = "List of items in the current page")
    private List<T> content;

    @Schema(description = "Current page number (1-based)", example = "1")
    private int page;

    @Schema(description = "Number of items per page", example = "10")
    private int pageSize;

    @Schema(description = "Total number of items across all pages", example = "42")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Whether there is a next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Whether there is a previous page", example = "false")
    private boolean hasPrevious;

    /**
     * Creates a paginated PageResponse from a full list.
     * Applies pagination (slicing) to the given list based on page and pageSize.
     *
     * @param allItems the complete list of items (pre-filtered)
     * @param page     the requested page number (1-based)
     * @param pageSize the number of items per page
     * @param <T>      the type of elements
     * @return a PageResponse containing only the items for the requested page
     */
    public static <T> PageResponse<T> of(List<T> allItems, int page, int pageSize) {
        if (allItems == null) {
            allItems = Collections.emptyList();
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }

        int currentPage = Math.max(page, 1);
        long totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allItems.size());

        List<T> content;
        if (startIndex >= allItems.size()) {
            content = new ArrayList<>();
        } else {
            content = new ArrayList<>(allItems.subList(startIndex, endIndex));
        }

        return PageResponse.<T>builder()
                .content(content)
                .page(currentPage)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(currentPage < totalPages)
                .hasPrevious(currentPage > 1)
                .build();
    }

    /**
     * Creates a non-paginated PageResponse containing all items in a single page.
     *
     * @param allItems the complete list of items
     * @param <T>      the type of elements
     * @return a PageResponse with all items and totalPages = 1
     */
    public static <T> PageResponse<T> ofAll(List<T> allItems) {
        if (allItems == null) {
            allItems = Collections.emptyList();
        }

        int size = allItems.size();
        return PageResponse.<T>builder()
                .content(new ArrayList<>(allItems))
                .page(1)
                .pageSize(size > 0 ? size : 1)
                .totalElements(size)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
