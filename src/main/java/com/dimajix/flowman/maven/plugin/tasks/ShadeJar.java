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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import lombok.val;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;


public class ShadeJar extends Task {
    public ShadeJar(FlowmanMojo mojo, Deployment deployment) throws MojoFailureException {
        super(mojo, deployment);

        mavenProject.getModel().setPackaging("jar");
    }

    public void shadeJar(List<Artifact> artifacts, File outputDirectory) throws MojoExecutionException {
        val currentProject = mavenSession.getCurrentProject();
        try {
            mavenSession.setCurrentProject(mavenProject);

            // Set and resolve dependencies
            mavenProject.setDependencies(toDependencies(artifacts));
            resolveDependencies(mavenProject);

            // Remove "provided" depdencies
            val newDeps = mavenProject.getArtifacts().stream()
                .filter(a -> !a.getScope().equals("provided"))
                .collect(Collectors.toSet());
            mavenProject.setArtifacts(newDeps);

            executeMojo(
                plugin(
                    groupId("org.apache.maven.plugins"),
                    artifactId("maven-shade-plugin"),
                    version("3.4.0")
                ),
                goal("shade"),
                configuration(
                    element(name("shadedClassifierName"), deployment.getName()),
                    element(name("outputDirectory"), outputDirectory.toString()),
                    element(name("keepDependenciesWithProvidedScope"), "false"),
                    element(name("transformers"),
                        element(name("transformer"), attribute("implementation", "org.apache.maven.plugins.shade.resource.ApacheLicenseResourceTransformer")),
                        element(name("transformer"), attribute("implementation", "org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformer")),
                        element(name("transformer"), attribute("implementation", "org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"))
                    ),
                    element(name("artifactSet"),
                        element(name("includes"),
                            element(name("include"), "*:*")
                        ),
                        element(name("excludes"),
                            element(name("exclude"), "com.dimajix.flowman:flowman-spark-dependencies:*")
                        )
                    ),
                    element(name("filters"),
                        element(name("filter"),
                            element(name("artifact"), "*:*"),
                            element(name("excludes"),
                                element(name("exclude"), "META-INF/MANIFEST.MF"),
                                element(name("exclude"), "META-INF/*.SF"),
                                element(name("exclude"), "META-INF/*.DAS"),
                                element(name("exclude"), "META-INF/*.RSA")
                            )
                        )
                    )
                ),
                executionEnvironment(
                    mavenProject,
                    mavenSession,
                    pluginManager
                )
            );
        }
        finally {
            mavenSession.setCurrentProject(currentProject);
        }
    }
}
