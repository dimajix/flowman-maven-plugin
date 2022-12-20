package com.dimajix.flowman.maven.fs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lombok.val;


public interface FileSystem {
    static List<FileSystem> getFileSystems() {
        val loader = ServiceLoader.load(FileSystem.class);
        return StreamSupport.stream(loader.spliterator(), false).collect(Collectors.toList());
    }
    static FileSystem getFileSystem(URI uri) {
        return getFileSystem(uri.getScheme());
    }
    static FileSystem getFileSystem(String scheme) {
        val fscheme = scheme != null ? scheme : "file";
        val loader = ServiceLoader.load(FileSystem.class);
        return StreamSupport.stream(loader.spliterator(), false)
            .filter(fs -> fs.getScheme().equals(fscheme))
            .findFirst()
            .orElseThrow(() -> new FileSystemNotFoundException("No filesystem for scheme '" + scheme + "'"));
    }


    String getScheme();
    void put(URI target, File source) throws IOException;
    void putAll(URI target, File sources) throws IOException;
    void delete(URI target, boolean recursive) throws IOException;
}
