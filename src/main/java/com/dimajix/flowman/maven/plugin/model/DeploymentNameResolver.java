package com.dimajix.flowman.maven.plugin.model;

import java.util.Map;

import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.val;
import lombok.var;

public class DeploymentNameResolver extends StdConverter<Map<String,Deployment>, Map<String,Deployment>> {
    @Override
    public Map<String, Deployment> convert(Map<String, Deployment> value) {
        for (var entry : value.entrySet()) {
            val name = entry.getKey();
            val deployment = entry.getValue();
            deployment.setName(name);
        }
        return value;
    }
}
