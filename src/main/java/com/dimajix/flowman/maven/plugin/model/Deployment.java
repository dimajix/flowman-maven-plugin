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

package com.dimajix.flowman.maven.plugin.model;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.impl.DistDeployment;
import com.dimajix.flowman.maven.plugin.impl.JarDeployment;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = false)
@JsonSubTypes({
    @JsonSubTypes.Type(name = "jar", value = JarDeployment.class),
    @JsonSubTypes.Type(name = "dist", value = DistDeployment.class)
})
public abstract class Deployment {
    @Setter
    @Getter
    private String name;

    @Getter
    @Setter
    @JsonProperty(value="flowman", required = false)
    private FlowmanSettings flowmanSettings = new FlowmanSettings();

    @Getter
    @Setter
    @JsonProperty(value="build", required = false)
    private BuildSettings buildSettings = new BuildSettings();

    abstract public void build(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException;

    abstract public void test(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException;

    abstract public void shell(FlowmanMojo mojo, File flow) throws MojoFailureException, MojoExecutionException;

    abstract public void pack(FlowmanMojo mojo) throws MojoFailureException, MojoExecutionException;
}
