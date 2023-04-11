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

package com.dimajix.flowman.maven.plugin.mojos;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import com.dimajix.flowman.maven.plugin.interpolation.StringInterpolator;
import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.model.Package;
import com.dimajix.flowman.maven.plugin.model.Descriptor;
import com.dimajix.flowman.maven.plugin.model.ObjectMapper;


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
    @Component
    private ArtifactResolver artifactResolver;

    @Getter
    @Component
    private DependencyResolver dependencyResolver;

    @Getter
    @Inject
    protected ProjectDependenciesResolver dependenciesResolver;

    @Getter
    @Component
    private ArtifactHandlerManager artifactHandlerManager;

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
                val interpolator = StringInterpolator.createInterpolator(mavenSession, mavenProject);
                cachedDescriptor = ObjectMapper.read(deploymentDescriptor, Descriptor.class, interpolator);
                for (var pkg : cachedDescriptor.getPackages()) {
                    pkg.init(this);
                }
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

    public MavenProject getCurrentMavenProject() {
        return mavenSession.getCurrentProject();
    }

    public List<Package> getPackages() throws MojoFailureException {
        return getDescriptor().getPackages();
    }
    public Package getPackage(String name) throws MojoExecutionException, MojoFailureException {
        val pkgs = getDescriptor().getPackages();
        if (StringUtils.isEmpty(name)) {
            return pkgs.iterator().next();
        }
        else {
            val result = pkgs.stream().filter(d -> d.getName().equals(name)).findFirst();
            if (!result.isPresent()) {
                throw new MojoExecutionException("Flowman package '" + name + "' found. Please check your 'deployment.yml'.");
            }
            return result.get();
        }
    }


    public List<Deployment> getDeployments() throws MojoFailureException {
        return getDescriptor().getDeployments();
    }
    public Deployment getDeployment(String name) throws MojoExecutionException, MojoFailureException {
        val deployments = getDescriptor().getDeployments();
        if (StringUtils.isEmpty(name)) {
            return deployments.iterator().next();
        }
        else {
            val result =  deployments.stream().filter(d -> d.getName().equals(name)).findFirst();
            if (!result.isPresent()) {
                throw new MojoExecutionException("Flowman deployment '" + name + "' found. Please check your 'deployment.yml'.");
            }
            return result.get();
        }
    }

    public File getFlowmanProject(String name) throws MojoExecutionException, MojoFailureException {
        val projects = getDescriptor().getProjects();
        if (StringUtils.isEmpty(name)) {
            return projects.iterator().next();
        }
        else {
            val result = projects.stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst();
            if (result.isPresent())
                return result.get();
            else
                throw new MojoExecutionException("Flowman project '" + name + "' not found. Please check your 'deployment.yml'.");
        }
    }

    protected MavenProject createMavenProject(Package pkg) throws MojoFailureException, MojoExecutionException {
        val mojoProject = getMavenProject();
        val artifact = pkg.getArtifact();

        val mojoBuild = mojoProject.getBuild();
        val build = mojoBuild.clone();
        build.setDirectory(new File(buildDirectory, pkg.getName()).toString());
        build.setOutputDirectory(new File(new File(buildDirectory, pkg.getName()), "resources").toString());

        val mavenProject = new MavenProject(mojoProject.getModel().clone());
        mavenProject.setOriginalModel(mojoProject.getModel());
        mavenProject.setBuild(build);
        mavenProject.setFile(mojoProject.getFile());
        mavenProject.setArtifact(artifact);
        mavenProject.setRemoteArtifactRepositories(mojoProject.getRemoteArtifactRepositories());
        mavenProject.setPluginArtifactRepositories(mojoProject.getPluginArtifactRepositories());
        mavenProject.setDistributionManagement(mojoProject.getDistributionManagement());

        val flowmanSettings = pkg.getEffectiveFlowmanSettings();
        val parent0 = flowmanSettings.resolveParent();
        importDependencyManagement(mavenProject, parent0);

        val dependencies = pkg.getDependencies();
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
            dep.setClassifier(StringUtils.isNotEmpty(artifact.getClassifier()) ? artifact.getClassifier() : null);
            dep.setVersion(artifact.getVersion());
            dep.setType(artifact.getExtension());
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
        importManagement(mavenProject.getModel(), depMgmt, true);
    }

    private void importManagement(Model target, DependencyManagement source, boolean overrideTarget) {
        Map<String, Dependency> dependencies = new LinkedHashMap<>();
        DependencyManagement depMgmt = target.getDependencyManagement();

        if ( depMgmt != null ) {
            for ( Dependency dependency : depMgmt.getDependencies() ) {
                dependencies.put( dependency.getManagementKey(), dependency );
            }
        }
        else {
            depMgmt = new DependencyManagement();
            target.setDependencyManagement( depMgmt );
        }

        for ( Dependency dependency : source.getDependencies() ) {
            String key = dependency.getManagementKey();
            if ( overrideTarget || !dependencies.containsKey( key ) ) {
                dependencies.put( key, dependency );
            }
        }

        depMgmt.setDependencies( new ArrayList<>( dependencies.values() ) );
    }
}
