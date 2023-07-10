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

package com.dimajix.flowman.maven.plugin.tasks;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.stream.Collectors;

import lombok.val;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.dimajix.flowman.maven.plugin.model.ExecutionSettings;
import com.dimajix.flowman.maven.plugin.model.Package;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;
import com.dimajix.flowman.maven.plugin.util.Collections;


public class RunArtifacts extends Task {
    private final File homeDirectory;
    private final File confDirectory;
    private final ExecutionSettings executionSettings;
    private static final String[] extraJavaArgs = {
        "-XX:+IgnoreUnrecognizedVMOptions",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
        "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
        "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
        "-Djdk.reflect.useDirectMethodHandle=false"
    };


    public RunArtifacts(FlowmanMojo mojo, MavenProject mavenProject, ExecutionSettings executionSettings) throws MojoFailureException {
        super(mojo, mavenProject);
        this.confDirectory = null;
        this.homeDirectory = null;
        this.executionSettings = executionSettings;
    }

    public RunArtifacts(FlowmanMojo mojo, MavenProject mavenProject, File homeDirectory, File confDirectory, ExecutionSettings executionSettings) throws MojoFailureException {
        super(mojo, mavenProject);
        this.homeDirectory = homeDirectory;
        this.confDirectory = confDirectory;
        this.executionSettings = executionSettings;
    }

    public void runTests(File projectDirectory) throws MojoExecutionException, MojoFailureException {
        run("com.dimajix.flowman.tools.exec.Driver", projectDirectory, "test", "run");
    }

    public void runShell(File projectDirectory) throws MojoExecutionException, MojoFailureException {
        run("com.dimajix.flowman.tools.shell.Shell", projectDirectory);
    }

    public void run(String mainClass, File projectDirectory, String... args) throws MojoExecutionException, MojoFailureException {
        // Construct classpath
        val depres = resolveDependencies();
        val classPath = new StringBuffer();
        depres.getResolvedDependencies().stream().forEach(dep -> {
            if (classPath.length() > 0)
                classPath.append(File.pathSeparator);
            classPath.append(dep.getArtifact().getFile());
        });

        // Collect arguments
        val allArgs = new LinkedList<String>();
        val args0 = Arrays.asList(
                "-classpath",
                classPath.toString(),
                mainClass,
                "-f", projectDirectory.toString()
            );
        allArgs.addAll(Arrays.stream(extraJavaArgs).collect(Collectors.toList()));
        allArgs.addAll(executionSettings.getJavaOptions());
        allArgs.addAll(args0);
        allArgs.addAll(executionSettings.getFlowmanOptions());
        executionSettings.getProfiles().forEach(p -> {
            allArgs.add("-P");
            allArgs.add(p);
        });
        executionSettings.getEnvironment().forEach(e -> {
            allArgs.add("-D");
            allArgs.add(e);
        });
        executionSettings.getConfig().forEach(c -> {
            allArgs.add("--conf");
            allArgs.add(c);
        });
        allArgs.addAll(Arrays.asList(args));

        val systemEnvironment = new HashMap<String,String>();
        systemEnvironment.put("FLOWMAN_HOME", homeDirectory != null ? homeDirectory.toString() : "");
        systemEnvironment.put("FLOWMAN_CONF_DIR", confDirectory != null ? confDirectory.toString() : "");
        systemEnvironment.putAll(Collections.splitSettings(executionSettings.getSystemEnvironment()));

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
                element(name("inheritIo"), "true"),
                element(name("environmentVariables"),
                    systemEnvironment.entrySet().stream().map(arg -> element(name(arg.getKey()), arg.getValue())).toArray(Element[]::new)
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
