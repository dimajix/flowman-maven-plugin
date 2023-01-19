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

package com.dimajix.flowman.maven.plugin.tasks;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.val;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.dimajix.flowman.maven.plugin.model.Package;
import com.dimajix.flowman.maven.plugin.mojos.FlowmanMojo;
import com.dimajix.flowman.maven.plugin.tasks.assembly.AssemblyDescriptor;
import com.dimajix.flowman.maven.plugin.util.Jackson;


public class AssembleDist extends Task {
    public AssembleDist(FlowmanMojo mojo, MavenProject mavenProject) throws MojoFailureException {
        super(mojo, mavenProject);
    }

    public void assemble(AssemblyDescriptor assemblyDescriptor, String finalName) throws MojoExecutionException {
        val buildDirectory = mavenProject.getBuild().getDirectory();
        val assemblyDescriptorFile = new File(buildDirectory, "assembly.xml");
        XmlMapper mapper = new XmlMapper(Jackson.newXMLFactory());
        try {
            mapper.writer().writeValue(assemblyDescriptorFile, assemblyDescriptor);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }

        executeMojo(
            plugin(
                groupId("org.apache.maven.plugins"),
                artifactId("maven-assembly-plugin"),
                version("3.4.2")
            ),
            goal("single"),
            configuration(
                element(name("attach"), "true"),
                element(name("appendAssemblyId"), "false"),
                element(name("tarLongFileMode"), "posix"),
                element(name("finalName"), finalName),
                element(name("descriptors"),
                    element(name("descriptor"), assemblyDescriptorFile.toString())
                ),
                element(name("archiverConfig"),
                    element(name("fileMode"), "0644"),
                    element(name("directoryMode"), "0755"),
                    element(name("defaultDirectoryMode"), "0755")
                )
            ),
            executionEnvironment(
                mavenProject,
                mavenSession,
                pluginManager
            )
        );

    }
}
