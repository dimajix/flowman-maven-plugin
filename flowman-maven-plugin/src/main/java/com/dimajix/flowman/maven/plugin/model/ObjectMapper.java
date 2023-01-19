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
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.val;
import lombok.var;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;

import com.dimajix.flowman.maven.plugin.interpolation.StringInterpolator;


public class ObjectMapper {
    private ObjectMapper() {
    }

    static com.fasterxml.jackson.databind.ObjectMapper getMapper() {
        val mapper = new com.fasterxml.jackson.databind.ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        return mapper;
    }

    static public <T> T read(File file, Class<T> valueType, FixedStringSearchInterpolator intp) throws IOException {
        val mapper = getMapper();
        val tree = mapper.readTree(file);
        return convertTree(tree, valueType, intp);
    }

    static public <T> T read(URL url, Class<T> valueType, FixedStringSearchInterpolator intp) throws IOException {
        val mapper = getMapper();
        val tree = mapper.readTree(url);
        return convertTree(tree, valueType, intp);
    }

    static public <T> T read(String spec, Class<T> valueType, FixedStringSearchInterpolator intp) throws IOException {
        val mapper = getMapper();
        val tree = mapper.readTree(spec);
        return convertTree(tree, valueType, intp);
    }

    static private <T> T convertTree(JsonNode tree, Class<T> valueType, FixedStringSearchInterpolator intp) throws JsonProcessingException {
        val newTree = applyThings(tree, intp);
        val mapper = getMapper();
        return mapper.treeToValue(newTree, valueType);
    }

    static private JsonNode applyThings(JsonNode node, FixedStringSearchInterpolator intp) {
        if (node.isObject()) {
            val obj = (ObjectNode)node;
            val newChildren = new HashMap<String,JsonNode>();
            for (var it = obj.fields(); it.hasNext(); ) {
                val namedChild = it.next();
                val newChild = applyThings(namedChild.getValue(), intp);
                newChildren.put(namedChild.getKey(), newChild);
            }
            obj.setAll(newChildren);
            return obj;
        }
        else if (node.isTextual()) {
            val text = node.asText();
            val newText = intp.interpolate(text);
            return TextNode.valueOf(newText);
        }
        else if (node.isArray()) {
            val ary = (ArrayNode)node;
            val newElements = new LinkedList<JsonNode>();
            for (var it = ary.elements(); it.hasNext(); ) {
                val elem = it.next();
                val newElement = applyThings(elem, intp);
                newElements.add(newElement);
            }
            ary.removeAll();
            ary.addAll(newElements);
            return ary;
        }
        else {
            return node;
        }
    }
}
