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

import com.dimajix.flowman.maven.plugin.model.*;
import com.dimajix.flowman.maven.plugin.util.Collections;
import lombok.Getter;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.composition.DefaultDependencyManagementImporter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.project.artifact.AttachedArtifact;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Getter
    @Inject
    protected ArtifactDescriptorReader artifactDescriptorReader;

    @Getter
    @Inject
    protected MavenProjectHelper mavenProjectHelper;

    /**
     * The output directory into which to copy the resources.
     */
    @Getter
    @Parameter( defaultValue = "${project.build.directory}", property="flowman.buildDirectory")
    protected File buildDirectory;
    @Parameter( defaultValue = "${project.directory}/deployment.yml", property="flowman.deploymentDescriptor")
    protected File deploymentDescriptor;

    private Descriptor cachedDescriptor = null;

    public Descriptor getDescriptor() throws MojoFailureException {
        if (cachedDescriptor == null) {
            try {
                cachedDescriptor = ObjectMapper.read(deploymentDescriptor, Descriptor.class);
                for (var deployment : cachedDescriptor.getDeployments()) {
                    deployment.init(this);
                }
            }
            catch(IOException ex) {
                throw new MojoFailureException(ex);
            }
        }
        return cachedDescriptor;
    }

    public MavenProject getCurrentProject() {
        return mavenSession.getCurrentProject();
    }

    protected MavenProject createMavenProject(Deployment deployment, Artifact outputArtifact) throws MojoFailureException, MojoExecutionException {
        val mojoProject = getMavenProject();
        val mojoArtifact = mojoProject.getArtifact();
        val artifact = outputArtifact != null ? outputArtifact : new AttachedArtifact(mojoArtifact, "jar", deployment.getName(), mojoArtifact.getArtifactHandler());
        val mavenProject = new MavenProject(mojoProject.getModel().clone());
        mavenProject.setOriginalModel(mojoProject.getModel());
        mavenProject.setBuild(mojoProject.getBuild().clone());
        mavenProject.setFile(mojoProject.getFile());
        mavenProject.setArtifact(artifact);
        mavenProject.setRemoteArtifactRepositories(mojoProject.getRemoteArtifactRepositories());
        mavenProject.setPluginArtifactRepositories(mojoProject.getPluginArtifactRepositories());

        val flowmanSettings = deployment.getEffectiveFlowmanSettings();
        val parent0 = flowmanSettings.resolveParent();
        importDependencyManagement(mavenProject, parent0);

        val dependencies = deployment.getDependencies();
        mavenProject.setDependencies(dependencies);

        return mavenProject;
    }

    public void attachArtifact(File artifactFile, String artifactClassifier) {
        mavenProjectHelper.attachArtifact(mavenProject, artifactFile, artifactClassifier);
    }

    public void attachArtifact(File artifactFile, String artifactType, String artifactClassifier) {
        mavenProjectHelper.attachArtifact(mavenProject, artifactType, artifactClassifier, artifactFile);
    }

    private void importDependencyManagement(MavenProject mavenProject, Artifact pom) throws MojoFailureException {
        val request = new ArtifactDescriptorRequest();
        val parent = new org.eclipse.aether.artifact.DefaultArtifact(pom.getGroupId(), pom.getArtifactId(), pom.getType(), pom.getVersion());
        request.setArtifact(parent);
        ArtifactDescriptorResult result;
        try {
            result = getArtifactDescriptorReader().readArtifactDescriptor(getMavenSession().getRepositorySession(), request);
        } catch (ArtifactDescriptorException e) {
            throw new MojoFailureException(e);
        }

        val depMgmt = new DependencyManagement();
        result.getManagedDependencies().forEach(dep0 -> {
            val dep = new Dependency();
            val artifact = dep0.getArtifact();
            dep.setGroupId(artifact.getGroupId());
            dep.setArtifactId(artifact.getArtifactId());
            dep.setClassifier(artifact.getClassifier());
            dep.setScope(dep0.getScope());
            if (dep0.getExclusions() != null) {
                val exclusions = dep0.getExclusions().stream().map(ex0 -> {
                    val ex = new Exclusion();
                    ex.setGroupId(ex0.getGroupId());
                    ex.setArtifactId(ex0.getArtifactId());
                    return ex;
                }).collect(Collectors.toList());
                dep.setExclusions(exclusions);
            }
            depMgmt.addDependency(dep);
        });
        val merger = new DefaultDependencyManagementImporter();
        merger.importManagement(mavenProject.getModel(), java.util.Collections.singletonList(depMgmt), null, null);
    }
}
