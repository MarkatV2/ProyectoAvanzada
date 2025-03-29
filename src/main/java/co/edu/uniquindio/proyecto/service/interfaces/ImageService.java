package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import org.bson.types.ObjectId;

import java.util.List;

public interface ImageService {

    public ImageResponse getImageById (String id);
    public ImageResponse registerImage(ImageUploadRequest request);
    public void deleteImage(String id);
    public List<ImageResponse> getAllImagesByReport (ObjectId reportId);
}
