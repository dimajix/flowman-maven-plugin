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


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.dimajix.flowman.maven.plugin.model.Package;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;

public class DeployArtifacts extends Task {
    public DeployArtifacts(FlowmanMojo mojo, MavenProject mavenProject) throws MojoFailureException {
        super(mojo, mavenProject);
    }

    public void deployArtifacts() throws MojoExecutionException {
        executeMojo(
            plugin(
                groupId("org.apache.maven.plugins"),
                artifactId("maven-deploy-plugin"),
                version("3.0.0")
            ),
            goal("deploy"),
            configuration(
            ),
            executionEnvironment(
                mavenProject,
                mavenSession,
                pluginManager
            )
        );
    }
}
