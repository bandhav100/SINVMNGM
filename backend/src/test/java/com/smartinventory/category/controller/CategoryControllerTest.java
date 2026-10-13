package com.smartinventory.category.controller;

import com.smartinventory.category.dto.CategoryRequestDto;
import com.smartinventory.category.dto.CategoryResponseDto;
import com.smartinventory.category.service.CategoryService;
import com.smartinventory.common.dto.PageResponseDto;
import com.smartinventory.common.exception.DuplicateResourceException;
import com.smartinventory.common.exception.GlobalExceptionHandler;
import com.smartinventory.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleResponse = new CategoryResponseDto(
                1L, "Electronics", "Devices and components", 4L, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/categories - Returns 201 Created")
    void createCategory_Success() throws Exception {
        String json = """
                {
                    "name": "Electronics",
                    "description": "Devices and components"
                }
                """;

        when(categoryService.createCategory(any(CategoryRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @DisplayName("POST /api/v1/categories - Duplicate name returns 409 Conflict")
    void createCategory_Duplicate_ReturnsConflict() throws Exception {
        String json = """
                {
                    "name": "Electronics",
                    "description": "Duplicate"
                }
                """;

        when(categoryService.createCategory(any(CategoryRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Category with name 'Electronics' already exists"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/v1/categories - Returns paginated 200 OK")
    void getCategories_Success() throws Exception {
        PageResponseDto<CategoryResponseDto> page = new PageResponseDto<>(
                List.of(sampleResponse), 0, 10, 1L, 1, true, true
        );
        when(categoryService.getCategories(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Electronics"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} - Not found returns 404")
    void getCategoryById_NotFound() throws Exception {
        when(categoryService.getCategoryById(99L))
                .thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

        mockMvc.perform(get("/api/v1/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Returns 204 No Content")
    void deleteCategory_Success() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1L);
    }
}
