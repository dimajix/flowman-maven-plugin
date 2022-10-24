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
import java.util.LinkedList;
import java.util.List;

import com.dimajix.flowman.maven.plugin.tasks.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.val;
import lombok.var;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;
import com.dimajix.flowman.maven.plugin.util.Collections;

public class JarDeployment extends Deployment {
    @JsonProperty(value="applicationPath", required = true)
    private String applicationPath;
    @JsonProperty(value="projectPath", required = false)
    private String projectPath;

    @Override
    public void build(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");

        // 1. Process sources
        val resources = new ProcessResources(mojo, this);
        resources.processResources(mojo.getDescriptor().getFlows(), outputDirectory);
        resources.processResources(new File("conf"), outputDirectory);
    }

    @Override
    public void test(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");

        // Execute Tests
        val run = new RunArtifacts(mojo, this, getFlowmanArtifacts(mojo), null, null);
        for (var flow : mojo.getDescriptor().getFlows()) {
            val projectDirectory = new File(outputDirectory, flow.getPath());
            run.runTests(projectDirectory);
        }
    }

    @Override
    public void pack(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = mojo.getFlowmanSettings(this);
        val buildSettings = mojo.getBuildSettings(this);
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");

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
    public void shell(FlowmanMojo mojo, File flow) throws MojoExecutionException, MojoFailureException {
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");
        val projectDirectory = new File(outputDirectory, flow.getPath());

        val run = new RunArtifacts(mojo, this, getFlowmanArtifacts(mojo), null, null);
        run.runShell(projectDirectory);
    }

    private List<Artifact> getFlowmanArtifacts(FlowmanMojo mojo) throws MojoFailureException {
        val buildSettings = mojo.getBuildSettings(this);
        val flowmanSettings = mojo.getFlowmanSettings(this);

        val flowmanTools = flowmanSettings.resolveTools();
        val flowmanSpark = flowmanSettings.resolveSparkDependencies();
        val dependencyArtifacts = buildSettings.resolveDependencies();
        return Collections.concat(Arrays.asList(flowmanTools, flowmanSpark), dependencyArtifacts);
    }
}
