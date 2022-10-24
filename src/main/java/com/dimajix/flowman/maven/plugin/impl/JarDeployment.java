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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;
import com.dimajix.flowman.maven.plugin.util.Collections;

public class JarDeployment extends AbstractDeployment {
    @JsonProperty(value="applicationPath", required = true)
    private String applicationPath;
    @JsonProperty(value="projectPath", required = false)
    private String projectPath;

    @Override
    public void build(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");

        val mavenProject = mojo.getCurrentProject();

        // 1. Process sources
        val resources = new ProcessResources(mojo, this, mavenProject);
        resources.processResources(mojo.getDescriptor().getFlows(), outputDirectory);
        resources.processResources(new File("conf"), outputDirectory);
    }

    @Override
    public void test(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");

        val mavenProject = mojo.getCurrentProject();

        // Execute Tests
        val run = new RunArtifacts(mojo, this, mavenProject, null, null);
        for (var flow : mojo.getDescriptor().getFlows()) {
            val projectDirectory = new File(outputDirectory, flow.getPath());
            run.runTests(projectDirectory);
        }
    }

    @Override
    public void pack(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");

        val mavenProject = mojo.getCurrentProject();

        // 2. Build Jar
        val jar = new BuildJar(mojo, this, mavenProject);
        jar.buildJar(outputDirectory, workDirectory);

        // 3. Shade Jar
        val shade = new ShadeJar(mojo, this, mavenProject);
        shade.shadeJar(workDirectory);

        val artifactFile = mavenProject.getArtifact().getFile();
        mojo.attachArtifact(artifactFile, "jar", getName());
    }

    @Override
    public void shell(FlowmanMojo mojo, File flow) throws MojoExecutionException, MojoFailureException {
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");
        val projectDirectory = new File(outputDirectory, flow.getPath());

        val mavenProject = mojo.getCurrentProject();

        val run = new RunArtifacts(mojo, this, mavenProject, null, null);
        run.runShell(projectDirectory);
    }

    @Override
    public List<Dependency> getDependencies(FlowmanMojo mojo) throws MojoFailureException {
        val buildSettings = mojo.getBuildSettings(this);
        val flowmanSettings = mojo.getFlowmanSettings(this);

        val flowmanTools = flowmanSettings.resolveTools();
        val flowmanSpark = flowmanSettings.resolveSparkDependencies();
        val dependencyArtifacts = buildSettings.resolveDependencies();
        val allDeps = Collections.concat(Arrays.asList(flowmanTools, flowmanSpark), dependencyArtifacts);

        return toDependencies(allDeps);
    }
}
