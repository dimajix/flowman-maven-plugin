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
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.dimajix.flowman.maven.plugin.model.Package;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;

public class ProcessResources extends Task {
    public ProcessResources(FlowmanMojo mojo, MavenProject mavenProject) throws MojoFailureException {
        super(mojo, mavenProject);
    }

    public void processResources(File sources, File outputDirectory) throws MojoExecutionException {
        processResources(Collections.singletonList(sources), outputDirectory);
    }

    public void processResources(Collection<File> sources, File outputDirectory) throws MojoExecutionException {
        executeMojo(
            plugin(
                groupId("org.apache.maven.plugins"),
                artifactId("maven-resources-plugin"),
                version("3.3.0")
            ),
            goal("resources"),
            configuration(
                element(name("propertiesEncoding"), "ISO-8859-1"),
                element(name("delimiters"), element(name("delimiter"), "@")),
                element(name("useDefaultDelimiters"), "false"),
                element(name("outputDirectory"), outputDirectory.toString()),
                element(name("resources"), sources.stream().map(src ->
                        element(name("resource"),
                            element(name("directory"), src.toString()),
                            element(name("targetPath"), new File(outputDirectory, src.getName()).toString()),
                            element(name("filtering"), "true")
                        )
                    ).toArray(Element[]::new)
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
