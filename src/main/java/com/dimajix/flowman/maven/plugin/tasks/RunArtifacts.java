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

package com.dimajix.flowman.maven.plugin.tasks;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import lombok.val;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;


public class RunArtifacts extends Task {
    private List<Artifact> artifacts;
    private File homeDirectory;
    private File confDirectory;


    public RunArtifacts(FlowmanMojo mojo, Deployment deployment, List<Artifact> artifacts) throws MojoFailureException {
        super(mojo, deployment);
        this.artifacts = artifacts;
    }

    public RunArtifacts(FlowmanMojo mojo, Deployment deployment, List<Artifact> artifacts, File homeDirectory, File confDirectory) throws MojoFailureException {
        super(mojo, deployment);
        this.artifacts = artifacts;
        this.homeDirectory = homeDirectory;
        this.confDirectory = confDirectory;
    }

    public void runTests(File projectDirectory) throws MojoExecutionException {
        run("com.dimajix.flowman.tools.exec.Driver", projectDirectory, "test", "run");
    }

    public void runShell(File projectDirectory) throws MojoExecutionException {
        run("com.dimajix.flowman.tools.shell.Shell", projectDirectory);
    }

    public void run(String mainClass, File projectDirectory, String... args) throws MojoExecutionException {
        val depres = resolveDependencies(artifacts);
        val classPath = new StringBuffer();
        depres.getResolvedDependencies().stream().forEach(dep -> {
            if (classPath.length() > 0)
                classPath.append(File.pathSeparator);
            classPath.append(dep.getArtifact().getFile());
        });

        val allArgs = new LinkedList<String>();
        val args0 = Arrays.asList(
            "-classpath",
            classPath.toString(),
            mainClass,
            "-f", projectDirectory.toString());
        allArgs.addAll(args0);
        allArgs.addAll(Arrays.asList(args));

        executeMojo(
            plugin(
                groupId("org.codehaus.mojo"),
                artifactId("exec-maven-plugin"),
                version("3.1.0")
            ),
            goal("exec"),
            configuration(
                element(name("addOutputToClasspath"), "false"),
                element(name("classpathScope"), "compile"),
                element(name("environmentVariables"),
                    element("FLOWMAN_HOME", homeDirectory != null ? homeDirectory.toString() : ""),
                    element("FLOWMAN_CONF_DIR", confDirectory != null ? confDirectory.toString() : "")
                ),
                element(name("executable"), "java"),
                element(name("arguments"),
                    allArgs.stream().map(arg -> element(name("argument"), arg)).toArray(Element[]::new)
                )
            ),
            executionEnvironment(
                mavenProject,
                mavenSession,
                pluginManager
            )
        );
    }
}
