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

package com.dimajix.flowman.maven.plugin.interpolation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Properties;

import lombok.val;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.fixed.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.fixed.PropertiesBasedValueSource;
import org.codehaus.plexus.util.cli.CommandLineUtils;


public class StringInterpolator {
    public static FixedStringSearchInterpolator createInterpolator(final MavenSession session, final MavenProject project) {
        return FixedStringSearchInterpolator.create(
            createRepositoryInterpolator(session),
            createCommandLinePropertiesInterpolator(session),
            createEnvInterpolator(),
            createProjectInterpolator(project)
        );
    }

    private static FixedStringSearchInterpolator createProjectInterpolator(final MavenProject project) {
        return FixedStringSearchInterpolator.create(
            new PrefixedPropertiesValueSource(InterpolationConstants.PROJECT_PROPERTIES_PREFIXES,
                project.getProperties(), true),
            new PrefixedObjectValueSource(InterpolationConstants.PROJECT_PREFIXES,
                project, true)
        );
    }

    private static FixedStringSearchInterpolator createRepositoryInterpolator(final MavenSession session) {
        val settingsProperties = new Properties();
        if (session.getSettings() != null) {
            settingsProperties.setProperty("localRepository", session.getSettings().getLocalRepository());
        }

        return FixedStringSearchInterpolator.create(new PropertiesBasedValueSource(settingsProperties));
    }

    private static FixedStringSearchInterpolator createCommandLinePropertiesInterpolator(final MavenSession session) {
        Properties commandLineProperties = System.getProperties();

        if (session != null) {
            commandLineProperties = new Properties();
            commandLineProperties.putAll(session.getSystemProperties());
            commandLineProperties.putAll(session.getUserProperties());
        }

        PropertiesBasedValueSource cliProps = new PropertiesBasedValueSource(commandLineProperties);
        return FixedStringSearchInterpolator.create(cliProps);
    }

    private static FixedStringSearchInterpolator createEnvInterpolator() {
        try {
            val envProps =
                new PrefixedPropertiesValueSource(Collections.singletonList("env."),
                    CommandLineUtils.getSystemEnvVars(false), true);
            return FixedStringSearchInterpolator.create(envProps);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
