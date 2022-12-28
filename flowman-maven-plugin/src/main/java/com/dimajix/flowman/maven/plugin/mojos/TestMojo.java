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

package com.dimajix.flowman.maven.plugin.mojos;

import java.io.File;
import java.util.Collections;

import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo( name = "test", threadSafe = true, defaultPhase = LifecyclePhase.TEST)
public class TestMojo extends FlowmanMojo {
    @Parameter( property="flowman.deployment")
    protected String deployment;
    @Parameter( property="flowman.project")
    protected String project;
    @Parameter( property="skipTests")
    protected Boolean skipTests = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!skipTests) {
            val deployments = StringUtils.isEmpty(deployment) ? getDeployments() : Collections.singletonList(getDeployment(deployment));

            for (var deployment : deployments) {
                getLog().info("");

                if (skipTests || deployment.isSkipTests()) {
                    getLog().info("-- Skipping test deployment '" + deployment.getName() + "'");
                }
                else {
                    getLog().info("-- Testing deployment '" + deployment.getName() + "'");

                    val mavenProject = createMavenProject(deployment);
                    val previousProject = mavenSession.getCurrentProject();
                    try {
                        mavenSession.setCurrentProject(mavenProject);
                        File flow = project != null ? getFlowmanProject(this.project) : null;
                        deployment.test(flow);
                    } finally {
                        mavenSession.setCurrentProject(previousProject);
                    }
                }
            }
        }
    }
}
