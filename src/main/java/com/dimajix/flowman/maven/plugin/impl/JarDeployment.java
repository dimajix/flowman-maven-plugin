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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.val;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;
import com.dimajix.flowman.maven.plugin.tasks.BuildJar;
import com.dimajix.flowman.maven.plugin.tasks.ProcessResources;
import com.dimajix.flowman.maven.plugin.tasks.ShadeJar;
import com.dimajix.flowman.maven.plugin.tasks.UnpackDist;
import com.dimajix.flowman.maven.plugin.util.Collections;

public class JarDeployment extends Deployment {
    @JsonProperty(value="applicationPath", required = true)
    private String applicationPath;
    @JsonProperty(value="projectPath", required = false)
    private String projectPath;

    @Override
    public void build(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = mojo.getFlowmanSettings(this);
        val buildSettings = mojo.getBuildSettings(this);
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");

        // 1. Process sources
        val resources = new ProcessResources(mojo, this);
        resources.processResources(mojo.getDescriptor().getFlows(), outputDirectory);
        resources.processResources(new File("conf"), outputDirectory);

        // 2. Build Jar
        val jar = new BuildJar(mojo, this);
        jar.buildJar(outputDirectory, workDirectory);

        // 3. Shade Jar
        val projectArtifact = mojo.getMavenProject().getArtifact();
        val flowmanArtifact = flowmanSettings.resolveTools();
        val dependencyArtifacts = buildSettings.resolveDependencies();
        val allArtifacts = Collections.concat(Arrays.asList(projectArtifact, flowmanArtifact), dependencyArtifacts);
        val shade = new ShadeJar(mojo, this);
        shade.shadeJar(allArtifacts, workDirectory);
    }

    @Override
    public void test(FlowmanMojo mojo) {

    }

    @Override
    public void pack(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {

    }

    @Override
    public void shell(FlowmanMojo mojo, File flow) {

    }
}
