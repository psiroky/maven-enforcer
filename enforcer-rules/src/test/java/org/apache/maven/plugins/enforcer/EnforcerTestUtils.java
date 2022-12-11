/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.enforcer;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.enforcer.utils.MockEnforcerExpressionEvaluator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyNode;

/**
 * The Class EnforcerTestUtils.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public final class EnforcerTestUtils {
    /**
     * Gets the maven session.
     *
     * @return the maven session
     */
    public static MavenSession getMavenSession() {
        PlexusContainer mock = mock(PlexusContainer.class);

        MavenExecutionRequest mer = mock(MavenExecutionRequest.class);
        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.setRepositorySession(any())).thenReturn(buildingRequest);
        when(mer.getProjectBuildingRequest()).thenReturn(buildingRequest);

        Properties systemProperties = new Properties();
        systemProperties.put("maven.version", "3.0");
        when(mer.getUserProperties()).thenReturn(new Properties());
        when(mer.getSystemProperties()).thenReturn(systemProperties);

        MavenExecutionResult meResult = mock(MavenExecutionResult.class);

        return new MavenSession(mock, new DefaultRepositorySystemSession(), mer, meResult);
    }

    /**
     * Gets the helper.
     *
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper() {
        return getHelper(new MockProject(), false);
    }

    /**
     * Gets the helper.
     *
     * @param mockExpression the mock expression
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper(boolean mockExpression) {
        return getHelper(new MockProject(), mockExpression);
    }

    /**
     * Gets the helper.
     *
     * @param project the project
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper(MavenProject project) {
        return getHelper(project, false);
    }

    private static RepositorySystem mockRepositorySystem() throws DependencyCollectionException {
        ArtifactType jarType = RepositoryUtils.newArtifactType("jar", new DefaultArtifactHandler("jar"));
        final DependencyNode node = new DefaultDependencyNode(
                new DefaultArtifact("groupId", "artifactId", "classifier", "jar", "version", jarType));
        node.setChildren(asList(
                new DefaultDependencyNode(
                        new DefaultArtifact("groupId", "artifact", "classifier", "jar", "1.0.0", jarType)),
                new DefaultDependencyNode(
                        new DefaultArtifact("groupId", "artifact", "classifier", "jar", "2.0.0", jarType))));

        RepositorySystem mockRepositorySystem = mock(RepositorySystem.class);
        when(mockRepositorySystem.collectDependencies(any(), any(CollectRequest.class)))
                .then(i -> new CollectResult(i.getArgument(1)).setRoot(node));
        return mockRepositorySystem;
    }

    /**
     * Gets the helper.
     *
     * @param project the project
     * @param mockExpression the mock expression
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper(MavenProject project, boolean mockExpression) {
        try {
            MavenSession session = getMavenSession();
            MojoExecution mockExecution = mock(MojoExecution.class);
            ExpressionEvaluator eval;
            if (mockExpression) {
                eval = new MockEnforcerExpressionEvaluator(session);
            } else {
                session.setCurrentProject(project);
                eval = new PluginParameterExpressionEvaluator(session, mockExecution);
            }

            PlexusContainer container = mock(PlexusContainer.class);
            when(container.lookup(RepositorySystem.class)).then(i -> mockRepositorySystem());

            ClassWorld classWorld = new ClassWorld("test", EnforcerTestUtils.class.getClassLoader());
            MojoDescriptor mojoDescriptor = new MojoDescriptor();
            mojoDescriptor.setRealm(classWorld.getClassRealm("test"));
            when(mockExecution.getMojoDescriptor()).thenReturn(mojoDescriptor);
            when(container.lookup(MojoExecution.class)).thenReturn(mockExecution);
            return new DefaultEnforcementRuleHelper(session, eval, new SystemStreamLog(), container);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the helper.
     *
     * @param project the project
     * @param eval the expression evaluator to use
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper(MavenProject project, ExpressionEvaluator eval) {
        MavenSession session = getMavenSession();
        return new DefaultEnforcementRuleHelper(session, eval, new SystemStreamLog(), null);
    }

    /**
     * New plugin.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @return the plugin
     */
    public static Plugin newPlugin(String groupId, String artifactId, String version) {
        InputSource inputSource = new InputSource();
        inputSource.setModelId("unit");

        Plugin plugin = new Plugin();
        plugin.setArtifactId(artifactId);
        plugin.setGroupId(groupId);
        plugin.setVersion(version);
        plugin.setLocation("version", new InputLocation(0, 0, inputSource));
        return plugin;
    }
}
