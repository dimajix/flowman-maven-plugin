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

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;

import com.dimajix.flowman.maven.plugin.model.BuildSettings;
import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.model.Descriptor;
import com.dimajix.flowman.maven.plugin.model.FlowmanSettings;
import com.dimajix.flowman.maven.plugin.model.ObjectMapper;
import com.dimajix.flowman.maven.plugin.util.Collections;

abstract public class FlowmanMojo extends AbstractMojo {
    /**
     * The project currently being build.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    @Getter
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    @Getter
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    @Getter
    @Component
    protected BuildPluginManager pluginManager;

    @Getter
    @Inject
    protected ProjectDependenciesResolver dependenciesResolver;

    /**
     * The output directory into which to copy the resources.
     */
    @Getter
    @Parameter( defaultValue = "${project.build.directory}", property="flowman.buildDirectory")
    protected File buildDirectory;
    @Parameter( defaultValue = "${project.directory}/deployment.yml", property="flowman.deploymentDescriptor")
    private File deploymentDescriptor;

    private Descriptor cachedDescriptor = null;

    public Descriptor getDescriptor() throws MojoFailureException {
        if (cachedDescriptor == null) {
            try {
                cachedDescriptor = ObjectMapper.read(deploymentDescriptor, Descriptor.class);
            }
            catch(IOException ex) {
                throw new MojoFailureException(ex);
            }
        }
        return cachedDescriptor;
    }

    public File getBuildDirectory(Deployment deployment) {
        return new File(this.buildDirectory, deployment.getName());
    }

    public FlowmanSettings getFlowmanSettings(Deployment deployment) throws MojoFailureException {
        val descriptorSettings = getDescriptor().getFlowmanSettings();
        val deploymentSettings = deployment.getFlowmanSettings();

        val result = new FlowmanSettings();
        result.setVersion(StringUtils.isNotEmpty(deploymentSettings.getVersion()) ? deploymentSettings.getVersion() : descriptorSettings.getVersion());
        result.setPlugins(Collections.concat(descriptorSettings.getPlugins(), deploymentSettings.getPlugins()));
        result.setEnvironment(Collections.concat(descriptorSettings.getEnvironment(), deploymentSettings.getEnvironment()));
        result.setConfig(Collections.concat(descriptorSettings.getConfig(), deploymentSettings.getConfig()));
        result.setProfiles(Collections.concat(descriptorSettings.getProfiles(), deploymentSettings.getProfiles()));
        return result;
    }

    public BuildSettings getBuildSettings(Deployment deployment) throws MojoFailureException {
        val descriptorSettings = getDescriptor().getBuildSettings();
        val deploymentSettings = deployment.getBuildSettings();

        val result = new BuildSettings();
        result.setProperties(Collections.concat(descriptorSettings.getProperties(), deploymentSettings.getProperties()));
        result.setDependencies(Collections.concat(descriptorSettings.getDependencies(), deploymentSettings.getDependencies()));
        return result;
    }

    public MavenProject getMavenProject(Deployment deployment) {
        // TODO:
        //   1. Apply SCM Settings
        //   2. Apply Maven properties
        //val project = new MavenProject(mavenProject);
        val project = mavenProject.clone();
        //project.getModel().addProperty("project.build.directory", getBuildDirectory().toString());
        return project;
    }
}
