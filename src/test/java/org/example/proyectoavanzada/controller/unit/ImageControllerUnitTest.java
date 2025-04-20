package org.example.proyectoavanzada.controller.unit;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.NoHandlerFoundException;

import co.edu.uniquindio.proyecto.controller.ImageController;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.exception.image.ImageNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.user.UserExceptionHandler;
import co.edu.uniquindio.proyecto.service.interfaces.ImageService;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ImageController.class)
@ContextConfiguration(classes = { ImageController.class, TestSecurityConfig.class })
@Import({ UserExceptionHandler.class, ErrorResponseBuilder.class, GlobalExceptionHandler.class })
class ImageControllerUnitTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ImageService imageService;

  private String imageId;
  private ImageResponse imageResponse;
  private ImageUploadRequest uploadRequest;

  @BeforeEach
  void setUp() {
    imageId = UUID.randomUUID().toString();
    imageResponse = new ImageResponse(imageId, "https://example.com/image.jpg", LocalDateTime.now());
    uploadRequest = new ImageUploadRequest("https://res.cloudinary.com/fdasf","123", "123");
  }

  @Test
  @DisplayName("✅ Debe retornar la imagen cuando el ID es válido")
  void shouldReturnImageWhenIdIsValid() throws Exception {
    // Arrange
    when(imageService.getImageById(imageId)).thenReturn(imageResponse);

    // Act & Assert
    mockMvc.perform(get("/api/v1/images/{imageId}", imageId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(imageId));
  }

  @Test
  @DisplayName("❌ Debe retornar 404 cuando la imagen no existe")
  void shouldReturnNotFoundWhenImageDoesNotExist() throws Exception {
    // Arrange
    when(imageService.getImageById(imageId)).thenThrow(new ImageNotFoundException("Imagen no encontrada"));

    // Act & Assert
    mockMvc.perform(get("/api/v1/images/{imageId}", imageId))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("Imagen no encontrada")));
  }

  @Test
  @DisplayName("✅ Debe registrar una imagen correctamente")
  void shouldRegisterImageSuccessfully() throws Exception {
    // Arrange
    when(imageService.registerImage(any(ImageUploadRequest.class))).thenReturn(imageResponse);

    // Act & Assert
    mockMvc.perform(post("/api/v1/images")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(uploadRequest)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/v1/images/" + imageId)))
        .andExpect(jsonPath("$.id").value(imageId));
  }

  @Test
  @DisplayName("❌ Debe retornar 400 si la URL está vacía")
  void shouldReturnBadRequestWhenUrlIsEmpty() throws Exception {
    // Arrange
    ImageUploadRequest invalidRequest = new ImageUploadRequest("","123","123");

    // Act & Assert
            mockMvc.perform(post("/api/v1/images")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("✅ Debe eliminar la imagen si el usuario es propietario o admin")
  void shouldDeleteImageSuccessfully() throws Exception {
    // Arrange
    Mockito.doNothing().when(imageService).deleteImage(imageId);

    // Act & Assert
    mockMvc.perform(delete("/api/v1/images/{id}", imageId))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("❌ Debe retornar 404 si la imagen no existe")
  void shouldReturnNotFoundWhenImageNotFound() throws Exception {
    // Arrange
    doThrow(new ImageNotFoundException("Imagen no encontrada")).when(imageService).deleteImage(imageId);

    // Act & Assert
    mockMvc.perform(delete("/api/v1/images/{id}", imageId))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("Imagen no encontrada")));
  }

}
