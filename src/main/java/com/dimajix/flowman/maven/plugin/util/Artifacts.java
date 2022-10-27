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

package com.dimajix.flowman.maven.plugin.util;

import lombok.val;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;


public class Artifacts {
    public static Artifact parseCoordinates(String coords) {
        return parseCoordinates(coords, "jar", null, null);
    }

    public static Artifact parseCoordinates(String coords, String type, String defaultClassifier, String defaultVersion) {
        val parts = coords.split(":");
        String groupId = null;
        String artifactId = null;
        String version = defaultVersion;
        String classifier = defaultClassifier;
        if (parts.length == 2) {
            if (version == null)
                throw new IllegalArgumentException("Missing artifact version: " + coords);
            groupId = parts[0];
            artifactId = parts[1];
        }
        else if (parts.length == 3) {
            groupId = parts[0];
            artifactId = parts[1];
            version = parts[2];
        }
        else if (parts.length == 4) {
            groupId = parts[0];
            artifactId = parts[1];
            classifier = parts[2];
            version = parts[3];
        }
        else {
            throw new IllegalArgumentException("Unsupported artifact: " + coords);
        }

        return new DefaultArtifact(
            groupId,
            artifactId,
            version,
            "compile",
            type,
            classifier,
            new DefaultArtifactHandler()
        );
    }
}
