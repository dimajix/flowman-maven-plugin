package com.dimajix.flowman.maven.plugin.tasks;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.val;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepositoryManager;

import com.dimajix.flowman.maven.plugin.model.BuildSettings;
import com.dimajix.flowman.maven.plugin.model.Deployment;
import com.dimajix.flowman.maven.plugin.model.FlowmanSettings;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;


public abstract class Task {
    protected MavenProject mavenProject;
    protected MavenSession mavenSession;
    protected BuildPluginManager pluginManager;
    ProjectDependenciesResolver dependenciesResolver;

    protected Deployment deployment;

    protected File buildDirectory;
    protected FlowmanSettings flowmanSettings;
    protected BuildSettings buildSettings;


    public Task(FlowmanMojo mojo, Deployment deployment) throws MojoFailureException {
        this.deployment = deployment;
        this.mavenProject = mojo.getMavenProject(deployment);
        this.mavenSession = mojo.getMavenSession();
        this.pluginManager = mojo.getPluginManager();
        this.dependenciesResolver = mojo.getDependenciesResolver();

        this.buildDirectory = mojo.getBuildDirectory(deployment);
        this.flowmanSettings = mojo.getFlowmanSettings(deployment);
        this.buildSettings = mojo.getBuildSettings(deployment);
    }

    protected Dependency toDependency(Artifact artifact) {
        val dep = new Dependency();
        dep.setGroupId(artifact.getGroupId());
        dep.setArtifactId(artifact.getArtifactId());
        dep.setVersion(artifact.getVersion());
        dep.setType(artifact.getType());
        dep.setScope(artifact.getScope());
        return dep;
    }
    protected List<Dependency> toDependencies(Artifact... artifacts) {
        return toDependencies(Arrays.asList(artifacts));
    }
    protected List<Dependency> toDependencies(List<Artifact> artifacts) {
        return artifacts.stream().map(this::toDependency)
            .collect(Collectors.toList());
    }

    protected DependencyResolutionResult resolveDependencies(List<Artifact> artifacts) throws MojoExecutionException {
        val execProject = new MavenProject(mavenProject);
        execProject.setArtifacts(null);
        execProject.setDependencyArtifacts(null);
        execProject.setDependencies(toDependencies(artifacts));
        return resolveDependencies(execProject);
    }
    protected DependencyResolutionResult resolveDependencies(MavenProject project) throws MojoExecutionException {
        RepositorySystemSession session = mavenSession.getRepositorySession();
        DependencyResolutionResult resolutionResult;

        try
        {
            DefaultDependencyResolutionRequest resolution = new DefaultDependencyResolutionRequest( project, session );
            resolutionResult = dependenciesResolver.resolve( resolution );
        }
        catch ( DependencyResolutionException e )
        {
            throw new MojoExecutionException(e);
        }

        Set<Artifact> artifacts = new LinkedHashSet<>();
        if ( resolutionResult.getDependencyGraph() != null )
        {
            RepositoryUtils.toArtifacts( artifacts, resolutionResult.getDependencyGraph().getChildren(),
                Collections.singletonList( project.getArtifact().getId() ), null );

            // Maven 2.x quirk: an artifact always points at the local repo, regardless whether resolved or not
            LocalRepositoryManager lrm = session.getLocalRepositoryManager();
            for ( Artifact artifact : artifacts )
            {
                if ( !artifact.isResolved() )
                {
                    String path = lrm.getPathForLocalArtifact( RepositoryUtils.toArtifact( artifact ) );
                    artifact.setFile( new File( lrm.getRepository().getBasedir(), path ) );
                }
            }
        }
        project.setResolvedArtifacts( artifacts );
        project.setArtifacts( artifacts );

        return resolutionResult;
    }
}
