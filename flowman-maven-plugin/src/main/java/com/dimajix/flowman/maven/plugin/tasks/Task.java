package com.dimajix.flowman.maven.plugin.tasks;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.val;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
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
import com.dimajix.flowman.maven.plugin.model.Package;
import com.dimajix.flowman.maven.plugin.model.FlowmanSettings;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;


public abstract class Task {
    protected MavenProject mavenProject;
    protected MavenSession mavenSession;
    protected BuildPluginManager pluginManager;
    protected ProjectDependenciesResolver dependenciesResolver;
    protected File buildDirectory;


    public Task(FlowmanMojo mojo, MavenProject mavenProject) {
        this.mavenProject = mavenProject;
        this.mavenSession = mojo.getMavenSession();
        this.pluginManager = mojo.getPluginManager();
        this.dependenciesResolver = mojo.getDependenciesResolver();

        this.buildDirectory = new File(mavenProject.getBuild().getDirectory());
    }

    public Artifact getArtifact() {
        return mavenProject.getArtifact();
    }

    protected DependencyResolutionResult resolveDependencies() throws MojoExecutionException {
        val repositorySystemSession = mavenSession.getRepositorySession();
        DependencyResolutionResult resolutionResult;

        try
        {
            DefaultDependencyResolutionRequest resolution = new DefaultDependencyResolutionRequest( mavenProject, repositorySystemSession );
            resolutionResult = dependenciesResolver.resolve( resolution );
        }
        catch ( DependencyResolutionException e )
        {
            throw new MojoExecutionException("Error resolving dependencies", e);
        }

        Set<Artifact> artifacts = new LinkedHashSet<>();
        if ( resolutionResult.getDependencyGraph() != null )
        {
            RepositoryUtils.toArtifacts( artifacts, resolutionResult.getDependencyGraph().getChildren(),
                Collections.singletonList( mavenProject.getArtifact().getId() ), null );

            // Maven 2.x quirk: an artifact always points at the local repo, regardless whether resolved or not
            LocalRepositoryManager lrm = repositorySystemSession.getLocalRepositoryManager();
            for ( Artifact artifact : artifacts )
            {
                if ( !artifact.isResolved() )
                {
                    String path = lrm.getPathForLocalArtifact( RepositoryUtils.toArtifact( artifact ) );
                    artifact.setFile( new File( lrm.getRepository().getBasedir(), path ) );
                }
            }
        }
        mavenProject.setResolvedArtifacts( artifacts );
        mavenProject.setArtifacts( artifacts );

        return resolutionResult;
    }
}
