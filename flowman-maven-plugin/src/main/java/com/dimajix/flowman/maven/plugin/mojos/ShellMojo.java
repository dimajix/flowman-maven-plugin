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

import lombok.val;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo( name = "shell", threadSafe = false, requiresProject = true)
public class ShellMojo extends FlowmanMojo {
    @Parameter(property="flowman.package")
    protected String pkg;
    @Parameter(property="flowman.project")
    protected String project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        val pkg = getPackage(this.pkg);
        val flow = getFlowmanProject(this.project);
        getLog().info("");
        getLog().info("-- Running shell for package '" + pkg.getName() + "'");

        val project = createMavenProject(pkg);
        val previousProject = mavenSession.getCurrentProject();
        try {
            mavenSession.setCurrentProject(project);
            pkg.shell(flow);
        }
        finally {
            mavenSession.setCurrentProject(previousProject);
        }
    }
}
