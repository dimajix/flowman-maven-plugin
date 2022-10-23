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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class Descriptor {
    @JsonProperty(value="flowman", required = true)
    private FlowmanSettings flowmanSettings = new FlowmanSettings();

    @JsonProperty(value="build", required = false)
    private BuildSettings buildSettings = new BuildSettings();

    @JsonProperty(value="scm", required = false)
    private ScmSettings scm = new ScmSettings();

    @JsonProperty(value="flows", required = true)
    private List<File> flows = Collections.emptyList();

    @JsonDeserialize(converter= DeploymentNameResolver.class)
    @JsonProperty(value="deployments", required = true)
    private Map<String,Deployment> deployments = Collections.emptyMap();

    public List<Deployment> getDeployments() {
        return deployments.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public Deployment getDeployment(String name) {
        if (StringUtils.isEmpty(name)) {
            return deployments.entrySet().iterator().next().getValue();
        }
        else {
            return deployments.get(name);
        }
    }

    public File getFlow(String name) {
        if (StringUtils.isEmpty(name)) {
            return getFlows().iterator().next();
        }
        else {
            return getFlows().stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst().get();
        }
    }
}
