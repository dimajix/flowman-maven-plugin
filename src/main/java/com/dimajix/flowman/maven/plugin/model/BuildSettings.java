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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

@Data
public class BuildSettings {
    @JsonProperty(value="properties", required = false)
    private List<String> properties = Collections.emptyList();

    @JsonProperty(value="dependencies", required = false)
    private List<String> dependencies = Collections.emptyList();

    @JsonProperty(value="exclusions", required = false)
    private List<String> exclusions = Collections.emptyList();


    public Map<String,String> getPropertiesMap() {
        return properties.stream().map(p -> StringUtils.split(p, "=", 2))
            .filter(k -> k.length == 2)
            .collect(Collectors.toMap(k -> k[0], v -> v[1]));
    }

    public List<Artifact> resolveDependencies() {
        return dependencies.stream().map(pi -> {
            val parts = pi.split(":");
            String groupId = null;
            String artifactId = null;
            String version = null;
            String type = "jar";
            if (parts.length == 3) {
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
                throw new IllegalArgumentException("Unsupported dependency artifact: " + pi);
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
