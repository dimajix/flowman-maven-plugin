/*
 * Copyright 2022 Kaya Kupferschmidt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
