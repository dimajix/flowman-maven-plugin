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

package com.dimajix.flowman.maven.plugin.tasks.assembly;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class FileSet {
    @JsonProperty(value = "directory", required = true)
    private String directory = "";
    @JsonProperty("outputDirectory")
    private String outputDirectory = "";
    @JsonProperty("fileMode")
    private String fileMode = "0644";
    @JsonProperty("directoryMode")
    private String directoryMode = "0755";
    @JsonProperty("include")
    @JacksonXmlElementWrapper(localName = "includes")
    private List<String> includes = Collections.emptyList();
    @JsonProperty("exclude")
    @JacksonXmlElementWrapper(localName = "excludes")
    private List<String> excludes = Collections.emptyList();
}
