package com.dimajix.flowman.maven.fs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import lombok.val;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LocalFileSystem implements FileSystem {
    @Override
    public String getScheme() {
        return "file";
    }

    @Override
    public void put(URI target, File source) throws IOException {
        val path = Paths.get(normalizeUri(target));
        Files.createDirectories(path.getParent());
        Files.copy(source.toPath(), path, REPLACE_EXISTING);
    }

    @Override
    public void putAll(URI target, File sources) throws IOException {
        val path = Paths.get(normalizeUri(target));
        Files.createDirectories(path);
        for (val src : Files.list(sources.toPath()).collect(Collectors.toList())) {
            val tgt = path.resolve(src.getFileName().toString());
            if (Files.isDirectory(src)) {
                putAll(tgt.toUri(), src.toFile());
            }
            else {
                Files.copy(src, tgt, REPLACE_EXISTING);
            }
        }
    }

    @Override
    public void delete(URI target, boolean recursive) throws IOException {
        val path = Paths.get(normalizeUri(target));
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                if (recursive) {
                    for (val src : Files.list(path).collect(Collectors.toList())) {
                        delete(src.toUri(), recursive);
                    }
                }
            }
            Files.delete(path);
        }
    }

    private URI normalizeUri(URI uri) throws IOException {
        try {
            return uri.getScheme() == null ? new URI("file:" + uri) : uri;
        }
        catch(URISyntaxException ex) {
            throw new IOException(ex);
        }
    }
}
