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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import lombok.val;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;

import com.dimajix.flowman.maven.fs.FileSystem;
import com.dimajix.flowman.maven.plugin.model.Package;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;

public class ResolveArtifact extends Task {
    private final Log log;
    private final ArtifactResolver artifactResolver;
    private final ArtifactHandlerManager artifactHandlerManager;

    public ResolveArtifact(FlowmanMojo mojo, MavenProject mavenProject) throws MojoFailureException {
        super(mojo, mavenProject);

        this.log = mojo.getLog();
        this.artifactResolver = mojo.getArtifactResolver();
        this.artifactHandlerManager = mojo.getArtifactHandlerManager();
    }

    public Artifact resolve(Artifact artifact) throws MojoExecutionException {
        ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler(artifact.getType());
        DefaultArtifactCoordinate artifactCoordinate = new DefaultArtifactCoordinate();
        artifactCoordinate.setGroupId(artifact.getGroupId());
        artifactCoordinate.setArtifactId(artifact.getArtifactId());
        artifactCoordinate.setVersion(artifact.getVersion());
        artifactCoordinate.setClassifier(artifact.getClassifier());
        artifactCoordinate.setExtension(artifactHandler.getExtension());

        val buildingRequest = mavenSession.getProjectBuildingRequest();

        try {
            if (artifact.getFile() == null || !artifact.getFile().exists()) {
                val result = artifactResolver.resolveArtifact(buildingRequest, artifactCoordinate);
                artifact.setFile(result.getArtifact().getFile());
                return result.getArtifact();
            }
            else {
                return artifact;
            }
        } catch (ArtifactResolverException e) {
            throw new MojoExecutionException(e);
        }
    }

    public void copy(Artifact artifact, URI targetLocation) throws MojoExecutionException {
        val artifact2 = resolve(artifact);

        // Copy artifact to target location
        try {
            val source = artifact2.getFile();
            val target = new URI(targetLocation + "/").resolve(source.getName());
            val fs = FileSystem.getFileSystem(target.getScheme());
            log.info("Copying " + source + " to " + target);
            fs.put(target, source);
        }
        catch(URISyntaxException ex) {
            throw new MojoExecutionException(ex);
        }
        catch(IOException ex) {
            throw new MojoExecutionException(ex);
        }
    }
}
