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

package com.dimajix.flowman.maven.plugin.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.val;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

import com.dimajix.flowman.maven.plugin.model.Deployment;


abstract public class AbstractDeployment extends Deployment {

    protected Dependency toDependency(Artifact artifact) {
        val dep = new Dependency();
        dep.setGroupId(artifact.getGroupId());
        dep.setArtifactId(artifact.getArtifactId());
        dep.setVersion(artifact.getVersion());
        dep.setType(artifact.getType());
        dep.setScope(artifact.getScope());
        return dep;
    }
    protected List<Dependency> toDependencies(Artifact... artifacts) {
        return toDependencies(Arrays.asList(artifacts));
    }
    protected List<Dependency> toDependencies(List<Artifact> artifacts) {
        return artifacts.stream().map(this::toDependency)
            .collect(Collectors.toList());
    }
}
