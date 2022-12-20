package com.dimajix.flowman.maven.aws;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import com.amazonaws.services.s3.AmazonS3Client;
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
        val s3Client = new AmazonS3Client();

        val bucket = getBucket(target);
        val path = getPath(target);

        s3Client.putObject(bucket, path, source);
    }

    @Override
    public void putAll(URI target, File sources) throws IOException {
        val s3Client = new AmazonS3Client();

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
        val s3Client = new AmazonS3Client();

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
