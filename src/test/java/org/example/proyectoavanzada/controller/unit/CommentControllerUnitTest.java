package org.example.proyectoavanzada.controller.unit;

import java.time.LocalDateTime;

import co.edu.uniquindio.proyecto.controller.CommentController;
import co.edu.uniquindio.proyecto.exception.comment.CommentNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.comment.CommentExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.user.UserExceptionHandler;
import co.edu.uniquindio.proyecto.service.interfaces.CommentService;
import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;

import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the CommentController.
 */
@WebMvcTest(CommentController.class)
@ContextConfiguration(classes = {CommentController.class, TestSecurityConfig.class})
@Import({CommentExceptionHandler.class, ErrorResponseBuilder.class, GlobalExceptionHandler.class,
        UserExceptionHandler.class})
class CommentControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    private CommentRequest commentRequest;
    private CommentResponse commentResponse;

    @BeforeEach
    void setUp() {
        commentRequest = new CommentRequest("Este es un comentario", "12345");
        commentResponse = new CommentResponse("1", "Juan", "userId123", "12345", "Este es un comentario", LocalDateTime.now());
    }


    @Test
    void testCreateComment() throws Exception {
        // Arrange
        when(commentService.createComment(any(CommentRequest.class))).thenReturn(commentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\": \"Este es un comentario\", \"reportId\": \"12345\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.userName").value("Juan"))
                .andExpect(jsonPath("$.comment").value("Este es un comentario"));

        verify(commentService, times(1)).createComment(any(CommentRequest.class));
    }


    @Test
    void testGetCommentById() throws Exception {
        // Arrange
        String commentId = "1";
        when(commentService.getCommentById(commentId)).thenReturn(commentResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/comments/{commentId}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.comment").value("Este es un comentario"))
                .andExpect(jsonPath("$.userName").value("Juan"));

        verify(commentService, times(1)).getCommentById(commentId);
    }

    @Test
    void testSoftDeleteComment() throws Exception {
        // Arrange
        String commentId = "1";
        when(commentService.softDeleteComment(commentId)).thenReturn(commentResponse);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.comment").value("Este es un comentario"));

        verify(commentService, times(1)).softDeleteComment(commentId);
    }

    @Test
    void testCreateCommentWithValidReport() throws Exception {
        // Arrange
        CommentRequest validRequest = new CommentRequest("Comentario sobre reporte", "report123");
        CommentResponse validResponse = new CommentResponse("2", "Carlos", "userId456", "report123", "Comentario sobre reporte", LocalDateTime.now());
        when(commentService.createComment(validRequest)).thenReturn(validResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\": \"Comentario sobre reporte\", \"reportId\": \"report123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.userName").value("Carlos"))
                .andExpect(jsonPath("$.comment").value("Comentario sobre reporte"));

        verify(commentService, times(1)).createComment(validRequest);
    }

    @Test
    void testGetCommentByIdNotFound() throws Exception {
        // Arrange
        String invalidCommentId = "9999";
        when(commentService.getCommentById(invalidCommentId)).thenThrow(new CommentNotFoundException("Comentario no encontrado"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/comments/{commentId}", invalidCommentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comentario no encontrado"));

        verify(commentService, times(1)).getCommentById(invalidCommentId);
    }

    @Test
    void testCreateCommentWithExcessiveLength() throws Exception {
        // Arrange
        String longComment = "a".repeat(801); // Exceso de 800 caracteres
        CommentRequest invalidRequest = new CommentRequest(longComment, "report123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\": \"" + longComment + "\", \"reportId\": \"report123\"}"))
                .andExpect(status().isBadRequest());
        verify(commentService, times(0)).createComment(any(CommentRequest.class));
    }




}
