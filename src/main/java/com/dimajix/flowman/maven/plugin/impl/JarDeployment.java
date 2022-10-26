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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.dimajix.flowman.maven.plugin.tasks.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.val;
import lombok.var;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.util.Collections;

public class JarDeployment extends AbstractDeployment {
    @JsonProperty(value="applicationPath", required = true)
    private String applicationPath;
    @JsonProperty(value="projectPath", required = false)
    private String projectPath;

    @Override
    public void build() throws MojoFailureException, MojoExecutionException {
        val outputDirectory = new File(this.outputDirectory, "META-INF/flowman");

        val mavenProject = mojo.getCurrentMavenProject();

        // 1. Process sources
        val resources = new ProcessResources(mojo, this, mavenProject);
        resources.processResources(mojo.getDescriptor().getProjects(), outputDirectory);
        resources.processResources(new File("conf"), outputDirectory);

        // Remove any plugins from default-namespace.yml
        val ns = new File(outputDirectory, "conf/default-namespace.yml");
        if (ns.exists() && ns.isFile()) {
            val mapper = new ObjectMapper(new YAMLFactory());
            try {
                JsonNode tree;
                try (val reader = new FileInputStream(ns)) {
                    tree = mapper.reader().readTree(reader);
                }
                if (tree.isObject() && tree.findValue("plugins") != null) {
                    val objectTree = (ObjectNode)tree;
                    objectTree.without("plugins");
                    mapper.writer().writeValue(ns, objectTree);
                }
            }
            catch(IOException ex) {
                throw new MojoFailureException(ex);
            }
        }
    }

    @Override
    public void test(File project) throws MojoFailureException, MojoExecutionException {
        val outputDirectory = new File(this.outputDirectory, "META-INF/flowman");
        val confDirectory = new File(outputDirectory, "conf");
        val projectDirectories = project != null ? java.util.Collections.singletonList(project) : mojo.getDescriptor().getProjects();

        val mavenProject = mojo.getCurrentMavenProject();

        // Execute Tests
        val run = new RunArtifacts(mojo, this, mavenProject, null, confDirectory);
        for (var flow : projectDirectories) {
            val projectDirectory = new File(outputDirectory, flow.getPath());
            run.runTests(projectDirectory);
        }
    }

    @Override
    public void pack() throws MojoFailureException, MojoExecutionException {
        val mavenProject = mojo.getCurrentMavenProject();

        // 2. Build Jar
        val jar = new BuildJar(mojo, this, mavenProject);
        jar.buildJar(outputDirectory, buildDirectory);

        // 3. Shade Jar
        val shade = new ShadeJar(mojo, this, mavenProject);
        shade.shadeJar("com.dimajix.flowman.tools.exec.Driver");

        val artifactFile = mavenProject.getArtifact().getFile();
        mojo.attachArtifact(artifactFile, "jar", getName());

        val log = mojo.getLog();
        log.info("");
        log.info(" > Run 'flowexec' via 'spark-submit " + artifactFile.getName() + " -f <project-directory> <flowman-command>'");
        log.info(" > Run 'flowshell' via 'spark-submit --class com.dimajix.flowman.tools.shell.Shell " + artifactFile.getName() + " -f <project-directory>'");
    }

    @Override
    public void shell(File flow) throws MojoExecutionException, MojoFailureException {
        val outputDirectory = new File(this.outputDirectory, "META-INF/flowman");
        val projectDirectory = new File(outputDirectory, flow.getPath());
        val confDirectory = new File(outputDirectory, "conf");

        val mavenProject = mojo.getCurrentMavenProject();

        val run = new RunArtifacts(mojo, this, mavenProject, null, confDirectory);
        run.runShell(projectDirectory);
    }

    @Override
    public List<Dependency> getDependencies() throws MojoFailureException {
        val flowmanSettings = getEffectiveFlowmanSettings();
        val buildSettings = getEffectiveBuildSettings();

        val flowmanTools = flowmanSettings.resolveTools();
        val flowmanSpark = flowmanSettings.resolveSparkDependencies();
        val dependencyArtifacts = buildSettings.resolveDependencies();
        val allDeps = Collections.concat(Arrays.asList(flowmanTools, flowmanSpark), dependencyArtifacts);

        return toDependencies(allDeps);
    }
}
