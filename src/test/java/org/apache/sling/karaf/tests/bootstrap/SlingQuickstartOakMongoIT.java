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
package org.apache.sling.karaf.tests.bootstrap;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.WrappedUrlProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.testcontainers.containers.GenericContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingQuickstartOakMongoIT extends AbstractSlingQuickstartOakTestSupport {

    private static GenericContainer mongoContainer;

    private static final String MONGO_CONTAINER_IMAGE_NAME = "mongo";

    @Configuration
    public Option[] configuration() {
        final boolean testcontainer = Boolean.parseBoolean(System.getProperty("mongod.testcontainer", "true"));
        final String host;
        final Integer port;
        if (testcontainer) {
            mongoContainer = new GenericContainer<>(MONGO_CONTAINER_IMAGE_NAME)
                .withExposedPorts(27017)
                .withStartupTimeout(Duration.ofMinutes(5));
            mongoContainer.start();
            host = mongoContainer.getContainerIpAddress();
            port = mongoContainer.getFirstMappedPort();
        } else {
            host = System.getProperty("mongod.host", "localhost");
            port = Integer.parseInt(System.getProperty("mongod.port", "27017"));
        }

        final String mongoUri = String.format("mongodb://%s:%s", host, port);
        return OptionUtils.combine(baseConfiguration(),
            editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot", "(wrap)"),
            editConfigurationFilePut("etc/org.apache.jackrabbit.oak.plugins.document.DocumentNodeStoreService.config", "mongouri", mongoUri),
            addSlingFeatures("sling-quickstart-oak-mongo"),
            wrappedBundle(mavenBundle().groupId("org.rnorth.duct-tape").artifactId("duct-tape").versionAsInProject()),
            wrappedBundle(mavenBundle().groupId("org.testcontainers").artifactId("testcontainers").versionAsInProject()).imports("org.junit.rules").overwriteManifest(WrappedUrlProvisionOption.OverwriteMode.MERGE)
        );
    }

    @Test
    public void testOrgApacheSlingJcrOakServer() {
        final Bundle bundle = findBundle("org.apache.sling.jcr.oak.server");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheJackrabbitOakStoreDocument() {
        final Bundle bundle = findBundle("org.apache.jackrabbit.oak-store-document");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheJackrabbitOakLucene() {
        final Bundle bundle = findBundle("org.apache.jackrabbit.oak-lucene");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgMongodbMongoJavaDriver() {
        final Bundle bundle = findBundle("org.mongodb.mongo-java-driver");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testComH2databaseH2Mvstore() {
        final Bundle bundle = findBundle("com.h2database.mvstore");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

}
