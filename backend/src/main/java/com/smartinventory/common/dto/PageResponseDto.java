package com.smartinventory.common.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageResponseDto<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {
    public static <T> PageResponseDto<T> fromPage(Page<T> page) {
        return new PageResponseDto<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }

    public static <T, R> PageResponseDto<R> fromPage(Page<T> page, java.util.function.Function<T, R> mapper) {
        return new PageResponseDto<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }

    public static <T> PageResponseDto<T> of(List<T> content, int pageNumber, int pageSize, long totalElements, int totalPages, boolean isFirst, boolean isLast) {
        return new PageResponseDto<>(content, pageNumber, pageSize, totalElements, totalPages, isFirst, isLast);
    }

    public static <T> PageResponseDto<T> from(Page<T> page) {
        return fromPage(page);
    }
}
