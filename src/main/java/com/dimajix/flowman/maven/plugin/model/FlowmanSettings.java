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

package com.dimajix.flowman.maven.plugin.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.val;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

@Data
public class FlowmanSettings {
    @JsonProperty(value="version", required = true)
    private String version;

    @JsonProperty(value="distribution", required = false)
    private String distribution;

    @JsonProperty(value="plugins", required = false)
    private List<String> plugins = Collections.emptyList();

    @JsonProperty(value="profiles", required = false)
    private List<String> profiles = Collections.emptyList();

    @JsonProperty(value="environment", required = false)
    private List<String> environment = Collections.emptyList();

    @JsonProperty(value="config", required = false)
    private List<String> config = Collections.emptyList();

    public Artifact resolveDist() {
        return new DefaultArtifact(
            "com.dimajix.flowman",
            "flowman-dist",
            version,
            "provided",
            "tar.gz",
            "bin",
            new DefaultArtifactHandler()
        );
    }

    public Artifact resolveParent() {
        return new DefaultArtifact(
            "com.dimajix.flowman",
            "flowman-parent",
            version,
            "import",
            "pom",
            null,
            new DefaultArtifactHandler()
        );
    }

    public Artifact resolveSparkDependencies() {
        return new DefaultArtifact(
            "com.dimajix.flowman",
            "flowman-spark-dependencies",
            version,
            "provided",
            "pom",
            null,
            new DefaultArtifactHandler()
        );
    }

    public Artifact resolveTools() {
        return new DefaultArtifact(
            "com.dimajix.flowman",
            "flowman-tools",
            version,
            "compile",
            "jar",
            null,
            new DefaultArtifactHandler()
        );
    }

    public List<Artifact> resolvePlugins() {
        return plugins.stream().map(pi -> {
            val parts = pi.split(":");
            String groupId = null;
            String artifactId = null;
            String version = this.version;
            String type = "jar";
            if (parts.length == 2) {
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
                type = parts[2];
                version = parts[3];
            }
            else {
                throw new IllegalArgumentException("Unsupported plugin artifact id " + pi);
            }

            return new DefaultArtifact(
                groupId,
                artifactId,
                version,
                "compile",
                type,
                null,
                new DefaultArtifactHandler()
            );
        }).collect(Collectors.toList());
    }
}
