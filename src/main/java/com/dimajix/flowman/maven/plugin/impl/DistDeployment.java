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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.val;
import lombok.var;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;
import com.dimajix.flowman.maven.plugin.tasks.ProcessResources;
import com.dimajix.flowman.maven.plugin.tasks.RunArtifacts;
import com.dimajix.flowman.maven.plugin.tasks.UnpackDist;


public class DistDeployment extends AbstractDeployment {
    @JsonProperty(value="installPath", required = true)
    private String installPath;

    @Override
    public void build(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");

        val mavenProject = mojo.getCurrentProject();

        // 1. Unpack Flowman
        val flowman = mojo.getFlowmanSettings(this);
        val dist = flowman.resolveDist();
        val unpack = new UnpackDist(mojo, this, mavenProject);
        unpack.unpack(Collections.singletonList(dist), workDirectory);

        // 2. Process sources
        val resources = new ProcessResources(mojo, this, mavenProject);
        resources.processResources(mojo.getDescriptor().getFlows(), outputDirectory);
        resources.processResources(new File("conf"), outputDirectory);
    }

    @Override
    public void test(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = mojo.getFlowmanSettings(this);

        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");
        val confDirectory = new File(outputDirectory, "conf");
        val homeDirectory = new File(workDirectory, "flowman-" + flowmanSettings.getVersion());

        val mavenProject = mojo.getCurrentProject();

        // 3. Execute Tests
        val run = new RunArtifacts(mojo, this, mavenProject, homeDirectory, confDirectory);
        for (var flow : mojo.getDescriptor().getFlows()) {
            val projectDirectory = new File(outputDirectory, flow.getPath());
            run.runTests(projectDirectory);
        }
    }

    @Override
    public void pack(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException {

    }

    @Override
    public void shell(FlowmanMojo mojo, File flow) throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = mojo.getFlowmanSettings(this);

        val workDirectory = mojo.getBuildDirectory(this);
        val outputDirectory = new File(workDirectory, "resources");
        val projectDirectory = new File(outputDirectory, flow.getPath());
        val confDirectory = new File(outputDirectory, "conf");
        val homeDirectory = new File(workDirectory, "flowman-" + flowmanSettings.getVersion());

        val mavenProject = mojo.getCurrentProject();

        val run = new RunArtifacts(mojo, this, mavenProject, homeDirectory, confDirectory);
        run.runShell(projectDirectory);
    }

    @Override
    public List<Dependency> getDependencies(FlowmanMojo mojo) throws MojoFailureException {
        val flowman = mojo.getFlowmanSettings(this);
        val flowmanTools = flowman.resolveTools();
        val flowmanSpark = flowman.resolveSparkDependencies();
        val allDeps = Arrays.asList(flowmanTools, flowmanSpark);

        return toDependencies(allDeps);
    }
}
