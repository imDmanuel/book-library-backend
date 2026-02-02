package com.imdmanuel.book_library.services;

import com.imdmanuel.book_library.payload.response.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    FileUploadResult uploadImage(MultipartFile file, String folder) throws IOException;

    FileUploadResult uploadRawFile(MultipartFile file, String folder) throws IOException;

    void deleteFile(String fileId) throws IOException;
}
