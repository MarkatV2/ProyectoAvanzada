package org.example.proyectoavanzada.service.unit;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.entity.image.Image;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.image.ImageNotFoundException;
import co.edu.uniquindio.proyecto.exception.image.InvalidImageException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.ImageRepository;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.service.implementations.ImageServiceImpl;
import co.edu.uniquindio.proyecto.service.mapper.ImageMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import co.edu.uniquindio.proyecto.entity.report.Report;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ImageMapper imageMapper;

    @InjectMocks
    private ImageServiceImpl imageService;

    private Image image;
    private ImageResponse imageResponse;
    private ImageUploadRequest uploadRequest;
    private ObjectId validId;
    private String validIdStr;
    private ObjectId reportId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        validId = new ObjectId();
        validIdStr = validId.toString();
        reportId = new ObjectId();

        image = new Image();
        image.setId(validId);
        image.setImageUrl("https://res.cloudinary.com/demo/image/upload/sample.jpg");
        image.setUploadDate(LocalDateTime.now());
        image.setReportId(reportId);
        image.setUserId(new ObjectId());

        imageResponse = new ImageResponse(
                validId.toString(),
                image.getImageUrl(),
                image.getUploadDate()
        );

        uploadRequest = new ImageUploadRequest(
                image.getImageUrl(),
                image.getReportId().toString(),
                image.getUserId().toString()
        );
    }

    @Test
    @DisplayName("✅ Obtener imagen por ID - Éxito")
    void testGetImageById_Success() {
        // Arrange
        when(imageRepository.findById(validId)).thenReturn(Optional.of(image));
        when(imageMapper.toImageResponse(image)).thenReturn(imageResponse);

        // Act
        ImageResponse result = imageService.getImageById(validIdStr);

        // Assert
        assertEquals(imageResponse, result);
        verify(imageRepository).findById(validId);
        verify(imageMapper).toImageResponse(image);
    }

    @Test
    @DisplayName("❌ Obtener imagen por ID - ID inválido")
    void testGetImageById_InvalidIdFormat() {
        // Act & Assert
        assertThrows(IdInvalidException.class, () -> imageService.getImageById("invalid_id"));
    }

    @Test
    @DisplayName("❌ Obtener imagen por ID - Imagen no encontrada")
    void testGetImageById_NotFound() {
        // Arrange
        when(imageRepository.findById(validId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ImageNotFoundException.class, () -> imageService.getImageById(validIdStr));
    }

    @Test
    @DisplayName("✅ Registrar imagen - Éxito")
    void testRegisterImage_Success() {
        // Arrange
        when(imageMapper.toImage(uploadRequest)).thenReturn(image);
        when(imageRepository.save(image)).thenReturn(image);
        when(imageMapper.toImageResponse(image)).thenReturn(imageResponse);

        // Act
        ImageResponse result = imageService.registerImage(uploadRequest);

        // Assert
        assertEquals(imageResponse, result);
        verify(imageMapper).toImage(uploadRequest);
        verify(imageRepository).save(image);
        verify(imageMapper).toImageResponse(image);
    }

    @Test
    @DisplayName("❌ Registrar imagen - URL inválida")
    void testRegisterImage_InvalidUrl() {
        // Arrange
        ImageUploadRequest invalidRequest = new ImageUploadRequest(
                "http://example.com/image.jpg",
                reportId.toString(),
                new ObjectId().toString()
        );

        // Act & Assert
        assertThrows(InvalidImageException.class, () -> imageService.registerImage(invalidRequest));
    }

    @Test
    @DisplayName("✅ Eliminar imagen - Éxito")
    void testDeleteImage_Success() {
        // Arrange
        when(imageRepository.findById(validId)).thenReturn(Optional.of(image));

        // Act
        imageService.deleteImage(validIdStr);

        // Assert
        verify(imageRepository).findById(validId);
        verify(imageRepository).delete(image);
    }

    @Test
    @DisplayName("❌ Eliminar imagen - ID inválido")
    void testDeleteImage_InvalidId() {
        // Act & Assert
        assertThrows(IdInvalidException.class, () -> imageService.deleteImage("invalid_id"));
    }

    @Test
    @DisplayName("❌ Eliminar imagen - Imagen no encontrada")
    void testDeleteImage_NotFound() {
        // Arrange
        when(imageRepository.findById(validId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ImageNotFoundException.class, () -> imageService.deleteImage(validIdStr));
    }


    @Test
    @DisplayName("✅ Obtener imágenes por reporte - Éxito")
    void testGetAllImagesByReport_Success() {
        // Arrange
        Report mockReport = Mockito.spy(Report.class);
        mockReport.setId(reportId);
        mockReport.setUserId(new ObjectId());

        when(reportRepository.findById(any())).thenReturn(Optional.of(mockReport));
        when(imageRepository.findAllByReportId(any())).thenReturn(List.of(image));
        when(imageMapper.toImageResponse(image)).thenReturn(imageResponse);

        // Act
        List<ImageResponse> result = imageService.getAllImagesByReport(reportId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(imageResponse, result.get(0));
        verify(reportRepository).findById(any());
        verify(imageRepository).findAllByReportId(any());
        verify(imageMapper).toImageResponse(image);
    }


    @Test
    @DisplayName("❌ Obtener imágenes por reporte - Reporte no encontrado")
    void testGetAllImagesByReport_ReportNotFound() {
        // Arrange
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReportNotFoundException.class, () -> imageService.getAllImagesByReport(reportId));
    }
}
