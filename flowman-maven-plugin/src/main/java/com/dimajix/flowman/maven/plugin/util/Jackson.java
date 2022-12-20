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

package com.dimajix.flowman.maven.plugin.util;


import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlFactoryBuilder;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.val;

public class Jackson {
    public static YAMLFactory newYAMLFactory() {
        return new YAMLFactoryBuilder(new YAMLFactory())
            .disable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS)
            .build();
    }
    public static XmlFactory newXMLFactory() {
        return new XmlFactoryBuilder(new XmlFactory())
            .build();
    }

    public static void mergeArray(JsonNode tree, String element, List<String> elements) {
        if (!elements.isEmpty()) {
            val configNode = (ArrayNode) tree.get(element);
            if (configNode == null) {
                val configNode1 = (ArrayNode)tree.withArray(element);
                elements.forEach(configNode1::add);
            }
            else {
                val currentValues = new HashSet<String>();
                configNode.forEach(n -> currentValues.add(n.textValue()));
                elements.forEach(e -> {
                    if (!currentValues.contains(e))
                        configNode.add(e);
                });
            }
        }
    }
}
