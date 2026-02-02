package com.imdmanuel.book_library.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.imdmanuel.book_library.payload.response.FileUploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService implements FileStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    @Override
    public FileUploadResult uploadImage(MultipartFile file, String folder) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image",
                        "transformation", "q_auto,f_auto,c_limit,w_1000"));
        return new FileUploadResult(
                uploadResult.get("secure_url").toString(),
                uploadResult.get("public_id").toString());
    }

    @Override
    public FileUploadResult uploadRawFile(MultipartFile file, String folder) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "raw"));
        return new FileUploadResult(
                uploadResult.get("secure_url").toString(),
                uploadResult.get("public_id").toString());
    }

    @Override
    public void deleteFile(String publicId) throws IOException {
        // Cloudinary destroy needs resource_type if it's not 'image'
        // For simplicity we try deleting as image first, or we'd need to store
        // resource_type
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
