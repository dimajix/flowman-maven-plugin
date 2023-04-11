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

package com.dimajix.flowman.maven.plugin.impl;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.val;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.tasks.ResolveArtifact;


public class CopyDeployment extends AbstractDeployment {
    @JsonProperty(value="package", required = true)
    private String sourcePackage;
    @JsonProperty(value="location", required = true)
    private String targetLocation;

    @Override
    public void deploy() throws MojoFailureException, MojoExecutionException {
        // Pull and copy artifact
        val mavenProject = mojo.getCurrentMavenProject();
        val pkg = mojo.getPackage(sourcePackage);
        val myArtifact = pkg.getArtifact();
        val pull = new ResolveArtifact(mojo, mavenProject);
        try {
            pull.copy(myArtifact, new URI(targetLocation));
        } catch (URISyntaxException ex) {
            throw new MojoExecutionException(ex);
        }
    }
}
