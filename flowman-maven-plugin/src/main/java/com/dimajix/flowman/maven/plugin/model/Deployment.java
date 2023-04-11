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

package com.dimajix.flowman.maven.plugin.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.dimajix.flowman.maven.plugin.impl.CopyDeployment;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = false)
@JsonSubTypes({
    @JsonSubTypes.Type(name = "copy", value = CopyDeployment.class)
})
public abstract class Deployment {
    @Setter
    @Getter
    private String name;

    abstract public void init(FlowmanMojo mojo);
    abstract public void deploy() throws MojoFailureException, MojoExecutionException;
}
