package com.example.carshop.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.nio.file.Path;
@Configuration
public class StorageProperties {
    private final Path directory;
    public StorageProperties(@Value("${app.upload-dir:uploads}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    public Path directory() { return directory; }
}
