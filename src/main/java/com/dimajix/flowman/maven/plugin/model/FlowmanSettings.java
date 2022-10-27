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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.val;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

import com.dimajix.flowman.maven.plugin.util.Artifacts;


@Data
public class FlowmanSettings {
    @JsonProperty(value="version", required = true)
    private String version;

    @JsonProperty(value="distribution", required = false)
    private String distribution;

    @JsonProperty(value="plugins", required = false)
    private List<String> plugins = Collections.emptyList();

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

    public List<Artifact> resolvePluginJars() {
        return resolvePlugins("jar", null);
    }

    public List<Artifact> resolvePluginDists() {
        return resolvePlugins("tar.gz", "bin");
    }

    private List<Artifact> resolvePlugins(String type, String classifier) {
        val builtinPluginRegex = Pattern.compile("flowman-([a-z0-9]+)");
        return plugins.stream()
            .map(d -> {
                val matcher = builtinPluginRegex.matcher(d);
                if (matcher.matches()) {
                    val pluginSuffix = matcher.group(1);
                    return "com.dimajix.flowman:flowman-plugin-" + pluginSuffix;
                }
                else {
                    return d;
                }
            })
            .map(d -> Artifacts.parseCoordinates(d, type, classifier, version))
            .collect(Collectors.toList());
    }
}
