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
package org.apache.sling.karaf.testing;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Dictionary;
import java.util.Objects;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.options.OptionalCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

public abstract class KarafTestSupport {

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected ConfigurationAdmin configurationAdmin;

    @Inject
    @Filter(timeout = 300000)
    BootFinished bootFinished;

    public static final String KARAF_GROUP_ID = "org.apache.karaf";

    public static final String KARAF_ARTIFACT_ID = "apache-karaf";

    public static final String KARAF_NAME = "Apache Karaf";

    public KarafTestSupport() {
    }

    protected static synchronized int findFreePort() {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // test support

    protected int httpPort() throws IOException {
        final Configuration configuration = configurationAdmin.getConfiguration("org.apache.felix.http");
        final Dictionary<String, Object> properties = configuration.getProperties();
        final Object port = properties.get("org.osgi.service.http.port");
        return Integer.parseInt(port.toString());
    }

    protected Bundle findBundle(final String symbolicName) {
        for (final Bundle bundle : bundleContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }
        return null;
    }

    // configuration support

    protected String karafGroupId() {
        return KARAF_GROUP_ID;
    }

    protected String karafArtifactId() {
        return KARAF_ARTIFACT_ID;
    }

    protected String karafName() {
        return KARAF_NAME;
    }

    protected Option addSlingFeatures(final String... features) {
        return features(maven().groupId("org.apache.sling").artifactId("org.apache.sling.karaf-features").type("xml").classifier("features").versionAsInProject(), features);
    }

    protected Option addFelixHttpFeature() {
        return features(maven().groupId("org.apache.karaf.features").artifactId("standard").type("xml").classifier("features").versionAsInProject(), "felix-http");
    }

    protected Option karafTestSupportBundle() {
        return streamBundle(
            bundle()
                .add(KarafTestSupport.class)
                .set(Constants.BUNDLE_MANIFESTVERSION, "2")
                .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.sling.karaf-integration-tests")
                .set(Constants.EXPORT_PACKAGE, "org.apache.sling.karaf.testing")
                .set(Constants.IMPORT_PACKAGE, "javax.inject, org.apache.karaf.features, org.ops4j.pax.exam, org.ops4j.pax.exam.options, org.ops4j.pax.exam.util, org.ops4j.pax.tinybundles.core, org.osgi.framework, org.osgi.service.cm")
                .build()
        ).start();
    }

    protected Option[] baseConfiguration() {
        final int rmiRegistryPort = findFreePort();
        final int rmiServerPort = findFreePort();
        final int sshPort = findFreePort();
        final int httpPort = findFreePort();
        final String unpackDirectory = String.format("%s/target/paxexam/%s", PathUtils.getBaseDir(), getClass().getSimpleName());
        final Option[] options = options(
            karafDistributionConfiguration()
                .frameworkUrl(maven().groupId(karafGroupId()).artifactId(karafArtifactId()).versionAsInProject().type("tar.gz"))
                .useDeployFolder(false)
                .name(karafName())
                .unpackDirectory(new File(unpackDirectory)),
            keepRuntimeFolder(),
            editConfigurationFilePut("etc/org.apache.sling.jcr.base.internal.LoginAdminWhitelist.config", "whitelist.bundles.regexp", "^PAXEXAM.*$|^org.apache.sling.(launchpad|junit).*$"),
            editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.rootLogger.level", "WARN"),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", Integer.toString(rmiRegistryPort)),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", Integer.toString(rmiServerPort)),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", Integer.toString(sshPort)),
            editConfigurationFilePut("etc/org.apache.felix.http.cfg", "org.osgi.service.http.port", Integer.toString(httpPort)),
            addSlingFeatures("sling-configs"),
            mavenBundle().groupId("org.ops4j.pax.tinybundles").artifactId("tinybundles").versionAsInProject(),
            mavenBundle().groupId("biz.aQute.bnd").artifactId("biz.aQute.bndlib").versionAsInProject(),
            karafTestSupportBundle(),
            jacoco()
        );
        if (JavaVersionUtil.getMajorVersion() >= 9) {
            return combine(options, java9plus());
        } else {
            return options;
        }
    }

    protected OptionalCompositeOption jacoco() {
        final String jacocoCommand = System.getProperty("jacoco.command");
        final VMOption option = Objects.nonNull(jacocoCommand) && !jacocoCommand.trim().isEmpty() ? vmOption(jacocoCommand) : null;
        return when(Objects.nonNull(option)).useOptions(option);
    }

    protected Option[] java9plus() {
        return options(
            vmOption("--add-reads=java.xml=java.logging"),
            vmOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
            vmOption("--patch-module"),
            vmOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" + System.getProperty("karaf.version") + ".jar"),
            vmOption("--patch-module"),
            vmOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + System.getProperty("karaf.version") + ".jar"),
            vmOption("--add-opens"),
            vmOption("java.base/java.security=ALL-UNNAMED"),
            vmOption("--add-opens"),
            vmOption("java.base/java.net=ALL-UNNAMED"),
            vmOption("--add-opens"),
            vmOption("java.base/java.lang=ALL-UNNAMED"),
            vmOption("--add-opens"),
            vmOption("java.base/java.util=ALL-UNNAMED"),
            vmOption("--add-opens"),
            vmOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
            vmOption("--add-opens"),
            vmOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
            vmOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
            vmOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
            vmOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
            vmOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
            vmOption("-classpath"),
            vmOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*")
        );
    }

}
