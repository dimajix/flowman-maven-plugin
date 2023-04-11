/*
 * Copyright 2022 The Flowman Authors
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

package com.dimajix.flowman.maven.azure;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import lombok.val;

import com.dimajix.flowman.maven.fs.FileSystem;


public class AzureBlobFileSystem implements FileSystem {
    @Override
    public String getScheme() {
        return "abfs";
    }

    @Override
    public void put(URI target, File source) throws IOException {
        val fileSystemClient = createClient(target);
        val targetFile0 = target.getPath();
        val targetFile = targetFile0.startsWith("/") ? targetFile0.substring(1) : targetFile0;
        val targetDirectory = new File(targetFile).getParentFile();

        fileSystemClient.createDirectoryIfNotExists(targetDirectory.toString());

        val fileClient = fileSystemClient.createFile(targetFile, true);
        fileClient.uploadFromFile(source.toString(), true);
    }

    @Override
    public void putAll(URI target, File sources) throws IOException {

    }

    @Override
    public void delete(URI target, boolean recursive) throws IOException {
        val fileSystemClient = createClient(target);
        val targetFile = target.getPath();
        val targetDirectory = new File(targetFile).getParent();

        val directoryClient = fileSystemClient.getDirectoryClient(targetDirectory);
        directoryClient.deleteFileIfExists(targetFile);

        if (recursive) {
            directoryClient.deleteSubdirectoryIfExists(targetFile);
        }
    }

    private DataLakeFileSystemClient createClient(URI uri) {
        val host = uri.getHost();
        val fileSystemName = uri.getUserInfo();
        val endpoint = "https://" + host;

        val defaultCredential = new DefaultAzureCredentialBuilder().build();

        val serviceClient = new DataLakeServiceClientBuilder()
            .credential(defaultCredential)
            .endpoint(endpoint)
            .buildClient();

        return serviceClient.getFileSystemClient(fileSystemName);
    }
}
