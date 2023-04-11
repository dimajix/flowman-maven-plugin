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

package com.dimajix.flowman.maven.plugin.tasks.assembly;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;


@Data
@JacksonXmlRootElement(namespace = "http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2", localName = "assembly")
public class AssemblyDescriptor {
    @JsonProperty("id")
    private String id = "bin";
    @JsonProperty("includeBaseDirectory")
    private Boolean includeBaseDirectory = true;
    @JsonProperty("format")
    @JacksonXmlElementWrapper(localName = "formats")
    private List<String> formats = Collections.emptyList();
    @JsonProperty("baseDirectory")
    private String baseDirectory = "";
    @JsonProperty("fileSet")
    @JacksonXmlElementWrapper(localName = "fileSets")
    private List<FileSet> fileSets = Collections.emptyList();
}
