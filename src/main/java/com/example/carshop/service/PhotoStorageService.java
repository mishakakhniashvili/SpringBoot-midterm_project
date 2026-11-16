package com.example.carshop.service;
import com.example.carshop.config.StorageProperties;
import com.example.carshop.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.*;
import org.springframework.transaction.support.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;
@Service
public class PhotoStorageService {
    private static final Map<String, String> TYPES = Map.of("image/png", "png", "image/jpeg", "jpg");
    private final Path directory;
    public PhotoStorageService(StorageProperties properties) throws IOException {
        directory = properties.directory(); Files.createDirectories(directory);
    }
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Photo is required");
        if (file.getSize() > 5 * 1024 * 1024) throw ApiException.badRequest("Photo must be at most 5 MB");
        String extension = file.getContentType() == null ? null : TYPES.get(file.getContentType());
        if (extension == null) throw ApiException.badRequest("Only JPEG and PNG photos are accepted");
        Path path = directory.resolve(UUID.randomUUID() + "." + extension);
        try {
            byte[] bytes = file.getBytes();
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw ApiException.badRequest("File is not a readable image");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!(extension.equals("jpg") ? format.equals("jpeg") : format.equals("png")))
                        throw ApiException.badRequest("Image contents do not match content type");
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000)
                        throw ApiException.badRequest("Image must be at most 20 megapixels");
                    if (reader.read(0) == null) throw ApiException.badRequest("Invalid image");
                } finally { reader.dispose(); }
            }
            Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
            // Database rollback must not leave a new orphan file behind.
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) deleteQuietly(path);
                    }
                });
            }
            return path.getFileName().toString();
        } catch (IOException ex) {
            deleteQuietly(path);
            throw ApiException.badRequest("Could not read or store photo");
        }
    }
    public Resource load(String filename) {
        Path path = resolve(filename);
        if (!Files.isRegularFile(path)) throw ApiException.notFound("Photo");
        return new FileSystemResource(path);
    }
    public void deleteAfterCommit(String filename) {
        if (filename == null) return;
        Path path = resolve(filename);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { deleteQuietly(path); }
        });
    }
    private Path resolve(String filename) {
        if (filename == null) throw ApiException.notFound("Photo");
        Path path = directory.resolve(filename).normalize();
        if (!path.getParent().equals(directory)) throw ApiException.badRequest("Invalid photo path");
        return path;
    }
    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException ex) { System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING, "Could not delete old photo", ex); }
    }
}
