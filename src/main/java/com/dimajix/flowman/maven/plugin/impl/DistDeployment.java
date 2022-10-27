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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.val;
import lombok.var;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import static com.dimajix.flowman.maven.plugin.util.Jackson.newYAMLFactory;

import com.dimajix.flowman.maven.plugin.tasks.AssembleDist;
import com.dimajix.flowman.maven.plugin.tasks.ProcessResources;
import com.dimajix.flowman.maven.plugin.tasks.RunArtifacts;
import com.dimajix.flowman.maven.plugin.tasks.UnpackDependencies;
import com.dimajix.flowman.maven.plugin.tasks.assembly.AssemblyDescriptor;
import com.dimajix.flowman.maven.plugin.tasks.assembly.FileSet;
import com.dimajix.flowman.maven.plugin.util.Artifacts;
import com.dimajix.flowman.maven.plugin.util.Jackson;


public class DistDeployment extends AbstractDeployment {
    @JsonProperty(value="baseDirectory", required = false)
    private String baseDirectory;
    @JsonProperty(value="installPath", required = false)
    private String installPath;

    @Override
    public void build() throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = getEffectiveFlowmanSettings();
        val mavenProject = mojo.getCurrentMavenProject();
        val buildDirectory = new File(mavenProject.getBuild().getDirectory());
        val outputDirectory = new File(mavenProject.getBuild().getOutputDirectory());

        // 1. Unpack Flowman
        val dist = flowmanSettings.resolveDist();
        val unpack = new UnpackDependencies(mojo, this, mavenProject);
        unpack.unpack(Collections.singletonList(dist), buildDirectory);

        // 2. Unpack and install additional plugins
        val plugins = flowmanSettings.resolvePluginDists();
        if (!plugins.isEmpty()) {
            // TODO: This assumes a certain directory structure in the tar.gz
            unpack.unpack(plugins, new File(buildDirectory, "flowman-" + flowmanSettings.getVersion()));
        }

        // 3. Process sources
        val resources = new ProcessResources(mojo, this, mavenProject);
        resources.processResources(mojo.getDescriptor().getProjects(), outputDirectory);
        resources.processResources(new File("conf"), outputDirectory);

        // 4. Create appropriate default-namespace.yml
        val ns = new File(outputDirectory, "conf/default-namespace.yml");
        try {
            val mapper = new ObjectMapper(newYAMLFactory());
            var objectTree = mapper.getNodeFactory().objectNode();

            // Parse existing file (if it exists)
            if (ns.exists() && ns.isFile()) {
                try (val reader = new FileInputStream(ns)) {
                    JsonNode tree = mapper.reader().readTree(reader);
                    if (tree.isObject())
                        objectTree = (ObjectNode)tree;
                }
            }

            // Add config, env & plugins
            val configValues = flowmanSettings.getConfig();
            Jackson.mergeArray(objectTree, "config", configValues);
            val envValues = flowmanSettings.getEnvironment();
            Jackson.mergeArray(objectTree, "environment", envValues);
            // TODO: Support plugins contained in artifacts
            val pluginNames = flowmanSettings.getPlugins()
                    .stream()
                    .filter(p -> !p.contains(":"))
                    .collect(Collectors.toList());
            Jackson.mergeArray(objectTree, "plugins", pluginNames);

            mapper.writer().writeValue(ns, objectTree);
        }
        catch(IOException ex) {
            throw new MojoFailureException(ex);
        }
    }

    @Override
    public void test(File project) throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = getEffectiveFlowmanSettings();
        val mavenProject = mojo.getCurrentMavenProject();
        val buildDirectory = new File(mavenProject.getBuild().getDirectory());
        val outputDirectory = new File(mavenProject.getBuild().getOutputDirectory());

        val confDirectory = new File(outputDirectory, "conf");
        // TODO: This assumes a certain directory structure in the tar.gz
        val homeDirectory = new File(buildDirectory, "flowman-" + flowmanSettings.getVersion());
        val projectDirectories = project != null ? java.util.Collections.singletonList(project) : mojo.getDescriptor().getProjects();

        // 3. Execute Tests
        val run = new RunArtifacts(mojo, this, mavenProject, homeDirectory, confDirectory);
        for (var flow : projectDirectories) {
            val projectDirectory = new File(outputDirectory, flow.getPath());
            run.runTests(projectDirectory);
        }
    }

    @Override
    public void pack() throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = getEffectiveFlowmanSettings();
        val mavenProject = mojo.getCurrentMavenProject();
        val buildDirectory = new File(mavenProject.getBuild().getDirectory());
        val outputDirectory = new File(mavenProject.getBuild().getOutputDirectory());
        val confDirectory = new File(outputDirectory, "conf");
        // TODO: This assumes a certain directory structure in the tar.gz
        val homeDirectory = new File(buildDirectory, "flowman-" + flowmanSettings.getVersion());
        val projectDirectories = mojo.getDescriptor().getProjects();

        val ns = new File(outputDirectory, "conf/default-namespace.yml");
        val plugins = new HashSet<String>();
        try {
            // Parse default-namespace-yml for plugins (if it exists)
            if (ns.exists() && ns.isFile()) {
                try (val reader = new FileInputStream(ns)) {
                    val mapper = new ObjectMapper(newYAMLFactory());
                    JsonNode tree = mapper.reader().readTree(reader);
                    if (tree.isObject()) {
                        val objectTree = (ObjectNode) tree;
                        val pluginNode = objectTree.get("plugins");
                        if (pluginNode != null && pluginNode.isArray()) {
                            pluginNode.forEach(n -> plugins.add(n.textValue()));
                        }
                    }
                }
            }

            // TODO: Support plugins contained in artifacts
            flowmanSettings.getPlugins()
                .stream()
                .filter(p -> !p.contains(":"))
                .forEach(plugins::add);
        }
        catch(IOException ex) {
            throw new MojoFailureException(ex);
        }


        val descriptor = new AssemblyDescriptor();
        descriptor.setId(getName());
        descriptor.setFormats(Collections.singletonList("tar.gz"));
        descriptor.setBaseDirectory(baseDirectory != null ? baseDirectory : mavenProject.getArtifactId() + "-" + mavenProject.getVersion());

        val fileSets = new LinkedList<FileSet>();
        // Flowman-dist
        fileSets.add(new FileSet(
            homeDirectory.toString(),
            "",
            "0644",
            "0755",
            Arrays.asList("**/*"),
            Arrays.asList("bin/*", "conf/*", "plugins/**", "examples/**")
        ));
        fileSets.add(new FileSet(
            homeDirectory.toString(),
            "",
            "0755",
            "0755",
            Arrays.asList("bin/*"),
            Collections.emptyList()
        ));
        // Config
        fileSets.add(new FileSet(
            confDirectory.toString(),
            "conf",
            "0644",
            "0755",
            Arrays.asList("conf/*"),
            Collections.emptyList()
        ));
        // Plugins
        plugins.stream()
            .filter(p -> !p.contains(":"))
            .forEach(plugin ->
                fileSets.add(new FileSet(
                    new File(homeDirectory, "plugins/" + plugin).toString(),
                    "plugins/" + plugin,
                    "0644",
                    "0755",
                    Collections.emptyList(),
                    Collections.emptyList()
                ))
            );
        // Projects
        projectDirectories.forEach(project ->
            fileSets.add(new FileSet(
                new File(outputDirectory, project.getPath()).toString(),
                "flows/" + project.getName(),
                "0644",
                "0755",
                Arrays.asList("**/*"),
                Collections.emptyList()
            ))
        );
        descriptor.setFileSets(fileSets);

        val assembler = new AssembleDist(mojo, this, mavenProject);
        assembler.assemble(descriptor, mavenProject.getArtifactId() + "-" + mavenProject.getVersion());

        mavenProject.getAttachedArtifacts().forEach(a -> mojo.attachArtifact(a.getFile(), a.getType(), a.getClassifier()));
    }

    @Override
    public void shell(File flow) throws MojoFailureException, MojoExecutionException {
        val flowmanSettings = getEffectiveFlowmanSettings();
        val mavenProject = mojo.getCurrentMavenProject();
        val buildDirectory = new File(mavenProject.getBuild().getDirectory());
        val outputDirectory = new File(mavenProject.getBuild().getOutputDirectory());

        val projectDirectory = new File(outputDirectory, flow.getPath());
        val confDirectory = new File(outputDirectory, "conf");
        // TODO: This assumes a certain directory structure in the tar.gz
        val homeDirectory = new File(buildDirectory, "flowman-" + flowmanSettings.getVersion());

        val run = new RunArtifacts(mojo, this, mavenProject, homeDirectory, confDirectory);
        run.runShell(projectDirectory);
    }

    @Override
    public List<Dependency> getDependencies() throws MojoFailureException {
        val flowmanSettings = getEffectiveFlowmanSettings();
        val flowmanTools = flowmanSettings.resolveTools();
        val flowmanSpark = flowmanSettings.resolveSparkDependencies();
        val allDeps = Arrays.asList(flowmanTools, flowmanSpark);

        return toDependencies(allDeps);
    }
}
