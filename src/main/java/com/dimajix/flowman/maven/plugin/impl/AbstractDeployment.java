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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.model.BuildSettings;
import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.model.ExecutionSettings;
import com.dimajix.flowman.maven.plugin.model.FlowmanSettings;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;
import com.dimajix.flowman.maven.plugin.util.Collections;


abstract public class AbstractDeployment extends Deployment {
    @JsonIgnore
    protected FlowmanMojo mojo;
    @Getter
    @JsonIgnore
    protected File buildDirectory;
    @Getter
    @JsonIgnore
    protected File outputDirectory;


    @Override
    public void init(FlowmanMojo mojo) throws MojoFailureException {
        this.mojo = mojo;
        buildDirectory =  new File(mojo.getBuildDirectory(), getName());
        outputDirectory = new File(buildDirectory, "resources");
    }

    @Override
    public FlowmanSettings getEffectiveFlowmanSettings() throws MojoFailureException {
        val descriptorSettings = mojo.getDescriptor().getFlowmanSettings();
        val result = new FlowmanSettings();
        result.setVersion(StringUtils.isNotEmpty(flowmanSettings.getVersion()) ? flowmanSettings.getVersion() : descriptorSettings.getVersion());
        result.setPlugins(Collections.concat(descriptorSettings.getPlugins(), flowmanSettings.getPlugins()));
        result.setEnvironment(Collections.concat(descriptorSettings.getEnvironment(), flowmanSettings.getEnvironment()));
        result.setConfig(Collections.concat(descriptorSettings.getConfig(), flowmanSettings.getConfig()));
        return result;
    }
    @Override
    public BuildSettings getEffectiveBuildSettings() throws MojoFailureException {
        val descriptorSettings = mojo.getDescriptor().getBuildSettings();

        val result = new BuildSettings();
        result.setProperties(Collections.concat(descriptorSettings.getProperties(), buildSettings.getProperties()));
        result.setDependencies(Collections.concat(descriptorSettings.getDependencies(), buildSettings.getDependencies()));
        result.setExclusions(Collections.concat(descriptorSettings.getExclusions(), buildSettings.getExclusions()));
        return result;
    }
    @Override
    public ExecutionSettings getEffectiveExecutionSettings(Deployment deployment) throws MojoFailureException {
        val descriptorSettings = mojo.getDescriptor().getExecutionSettings();

        val result = new ExecutionSettings();
        result.setEnvironment(Collections.concat(descriptorSettings.getEnvironment(), executionSettings.getEnvironment()));
        result.setConfig(Collections.concat(descriptorSettings.getConfig(), executionSettings.getConfig()));
        result.setProfiles(Collections.concat(descriptorSettings.getProfiles(), executionSettings.getProfiles()));
        return result;
    }

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
