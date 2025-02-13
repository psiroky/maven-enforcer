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

import java.util.Collections;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class BanDependencyManagementScopeTest {
    @Test
    void testGetViolatingDependencies() throws EnforcerRuleException, InvalidVersionSpecificationException {
        BanDependencyManagementScope rule = new BanDependencyManagementScope();
        DependencyManagement depMgmt = new DependencyManagement();
        Dependency depWithoutScope = createDependency("myGroup", "artifact1", null);
        Dependency depWithScope = createDependency("myGroup", "artifact2", "1.1.0", "provided");
        Dependency depWithImportScope = createDependency("myGroup", "artifact3", "1.1.0", "import");
        Dependency excludedDepWithScope = createDependency("myGroup", "artifact4", "1.1.0", "provided");
        depMgmt.addDependency(depWithoutScope);
        depMgmt.addDependency(depWithScope);
        depMgmt.addDependency(depWithImportScope);
        depMgmt.addDependency(excludedDepWithScope);
        rule.setExcludes(Collections.singletonList("*:artifact4"));
        MatcherAssert.assertThat(
                rule.getViolatingDependencies(new SystemStreamLog(), depMgmt), Matchers.contains(depWithScope));
    }

    static Dependency createDependency(String groupId, String artifactId, String version) {
        return createDependency(groupId, artifactId, version, null);
    }

    static Dependency createDependency(String groupId, String artifactId, String version, String scope) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        if (version != null) {
            dependency.setVersion(version);
        }
        if (scope != null) {
            dependency.setScope(scope);
        }
        return dependency;
    }
}
