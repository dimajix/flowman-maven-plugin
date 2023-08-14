/*
 * Copyright 2022 The Flowman Authors
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import static com.dimajix.flowman.maven.plugin.util.Jackson.newYAMLFactory;

import com.dimajix.flowman.maven.plugin.tasks.BuildJar;
import com.dimajix.flowman.maven.plugin.tasks.ProcessResources;
import com.dimajix.flowman.maven.plugin.tasks.RunArtifacts;
import com.dimajix.flowman.maven.plugin.tasks.ShadeJar;
import com.dimajix.flowman.maven.plugin.util.Jackson;


public class JarPackage extends AbstractPackage {
    @JsonProperty(value="includeProjects", required = false)
    private boolean includeProjects = true;

    @Override
    public String getType() {
        return "jar";
    }


    @Override
    public void build() throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = getEffectiveFlowmanSettings();
        val mavenProject = mojo.getCurrentMavenProject();
        val outputDirectory = new File(getOutputDirectory(), "META-INF/flowman");

        // 1. Process sources
        val resources = new ProcessResources(mojo, mavenProject);
        resources.processResources(mojo.getDescriptor().getProjects(), outputDirectory);
        resources.processResources(mojo.getDescriptor().getResources(), outputDirectory);
        resources.processResources(new File("conf"), outputDirectory);

        // Create appropriate default-namespace.yml
        val ns = new File(outputDirectory, "conf/default-namespace.yml");
        try {
            val mapper = new ObjectMapper(newYAMLFactory());
            ObjectNode objectTree = mapper.getNodeFactory().objectNode();

            // Parse existing file (if it exists)
            if (ns.exists() && ns.isFile()) {
                try (val reader = new FileInputStream(ns)) {
                    JsonNode tree = mapper.reader().readTree(reader);
                    if (tree.isObject())
                        objectTree = (ObjectNode)tree;
                }
                // Remove plugins
                if (objectTree.findValue("plugins") != null) {
                    objectTree.without("plugins");
                }
            }
            else {
                ns.getParentFile().mkdirs();
            }

            // Add config & env
            val configValues = flowmanSettings.getConfig();
            Jackson.mergeArray(objectTree, "config", configValues);
            val envValues = flowmanSettings.getEnvironment();
            Jackson.mergeArray(objectTree, "environment", envValues);

            mapper.writer().writeValue(ns, objectTree);
        }
        catch(IOException ex) {
            throw new MojoFailureException(ex);
        }
    }

    @Override
    public void test(File project) throws MojoFailureException, MojoExecutionException {
        val mavenProject = mojo.getCurrentMavenProject();
        val outputDirectory = new File(getOutputDirectory(), "META-INF/flowman");
        val confDirectory = new File(outputDirectory, "conf");
        val projectDirectories = project != null ? java.util.Collections.singletonList(project) : mojo.getDescriptor().getProjects();

        // Execute Tests
        val run = new RunArtifacts(mojo, mavenProject, null, confDirectory, getEffectiveExecutionSettings());
        for (File flow : projectDirectories) {
            val projectDirectory = new File(outputDirectory, flow.getPath());
            run.runTests(projectDirectory);
        }
    }

    @Override
    public void pack() throws MojoFailureException, MojoExecutionException {
        val mavenProject = mojo.getCurrentMavenProject();
        val buildDirectory = getBuildDirectory();
        val outputDirectory = getOutputDirectory();
        List<File> exclusions = includeProjects ?
            Collections.emptyList() :
            Stream.concat(mojo.getDescriptor().getProjects().stream(), mojo.getDescriptor().getResources().stream()).map(src ->
                new File("META-INF/flowman", src.toString())
            ).collect(Collectors.toList());

        // 2. Build Jar
        val jar = new BuildJar(mojo, mavenProject);
        jar.buildJar(outputDirectory, buildDirectory, exclusions);

        // 3. Shade Jar
        val shade = new ShadeJar(mojo, mavenProject, getEffectiveBuildSettings());
        shade.shadeJar("com.dimajix.flowman.tools.exec.Driver", getName());

        val artifact = mavenProject.getArtifact();
        mojo.attachArtifact(artifact.getFile(), artifact.getType(), getName());

        val log = mojo.getLog();
        log.info("");
        log.info(" > Run 'flowexec' via 'spark-submit " + artifact.getFile().getName() + " -f <project-directory> <flowman-command>'");
        log.info(" > Run 'flowshell' via 'spark-submit --class com.dimajix.flowman.tools.shell.Shell " + artifact.getFile().getName() + " -f <project-directory>'");
    }

    @Override
    public void shell(File project) throws MojoExecutionException, MojoFailureException {
        val mavenProject = mojo.getCurrentMavenProject();
        val outputDirectory = new File(getOutputDirectory(), "META-INF/flowman");
        val projectDirectory = new File(outputDirectory, project.getPath());
        val confDirectory = new File(outputDirectory, "conf");

        val run = new RunArtifacts(mojo, mavenProject, null, confDirectory, getEffectiveExecutionSettings());
        run.runShell(projectDirectory);
    }

    @Override
    public void push() throws MojoFailureException, MojoExecutionException {
        // The jar will be pushed to Nexus via the root Maven project
    }

    @Override
    public List<Dependency> getDependencies() throws MojoFailureException {
        val flowmanSettings = getEffectiveFlowmanSettings();
        val buildSettings = getEffectiveBuildSettings();

        val flowmanTools = flowmanSettings.resolveTools();
        val flowmanSpark = flowmanSettings.resolveSparkDependencies();
        val flowmanPlugins = flowmanSettings.resolvePluginJars();
        val dependencies = buildSettings.resolveDependencies();

        val allDeps = new LinkedList<Artifact>();
        allDeps.add(flowmanTools);
        allDeps.add(flowmanSpark);
        allDeps.addAll(dependencies);
        allDeps.addAll(flowmanPlugins);

        return toDependencies(allDeps);
    }
}
