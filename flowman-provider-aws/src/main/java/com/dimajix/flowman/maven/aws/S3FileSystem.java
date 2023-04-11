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

package com.dimajix.flowman.maven.aws;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import lombok.val;
import lombok.var;

import com.dimajix.flowman.maven.fs.FileSystem;


public class S3FileSystem implements FileSystem {
    @Override
    public String getScheme() {
        return "s3";
    }

    @Override
    public void put(URI target, File source) throws IOException {
        val s3Client = AmazonS3ClientBuilder.defaultClient();

        val bucket = getBucket(target);
        val path = getPath(target);

        s3Client.putObject(bucket, path, source);
    }

    @Override
    public void putAll(URI target, File sources) throws IOException {
        val s3Client = AmazonS3ClientBuilder.defaultClient();

        val tm = TransferManagerBuilder.standard().withS3Client(s3Client).build();

        val bucket = getBucket(target);
        val path = getPath(target);
        val upload = tm.uploadDirectory(bucket, path, sources,true);
        try {
            upload.waitForCompletion();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void delete(URI target, boolean recursive) throws IOException {
        val s3Client = AmazonS3ClientBuilder.defaultClient();

        val bucket = getBucket(target);
        val path = getPath(target);

        val listObjectsRequest = new ListObjectsRequest()
            .withBucketName(bucket)
            .withPrefix(path);

        var objectListing = s3Client.listObjects(listObjectsRequest);

        while (true) {
            for (val objectSummary : objectListing.getObjectSummaries()) {
                s3Client.deleteObject(bucket, objectSummary.getKey());
            }
            if (objectListing.isTruncated()) {
                objectListing = s3Client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }

        if (s3Client.doesObjectExist(bucket, path)) {
            s3Client.deleteObject(bucket, path);
        }
    }

    private String getBucket(URI uri) {
        val host = uri.getHost();
        if (host != null) {
            return host;
        }

        val bucket = Arrays.stream(uri.getPath().split("/"))
            .filter(s -> !s.isEmpty())
            .findFirst();
        if (bucket.isPresent())
            return bucket.get();
        else
            throw new IllegalArgumentException("URI does not contain bucket: " + uri);
    }

    private String getPath(URI uri) {
        val path = getPath0(uri);
        if (path.startsWith("/"))
            return path.substring(1);
        else
            return path;
    }
    private String getPath0(URI uri) {
        val host = uri.getHost();
        if (host != null) {
            return uri.getPath();
        }

        val parts = Arrays.stream(uri.getPath().split("/"))
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
        val path = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length))
            .reduce((l,r) -> l + "/" + r);

        if (path.isPresent())
            return path.get();
        else
            throw new IllegalArgumentException("URI does not contain path: " + uri);
    }
}
