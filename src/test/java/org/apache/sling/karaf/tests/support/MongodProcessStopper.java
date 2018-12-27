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
package org.apache.sling.karaf.tests.support;

import java.util.HashSet;
import java.util.Set;

import de.flapdoodle.embed.mongo.MongodProcess;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class MongodProcessStopper extends RunListener {

    private static Set<MongodProcess> processes = new HashSet<>();

    public MongodProcessStopper() {
    }

    public static void add(final MongodProcess process) {
        processes.add(process);
    }

    @Override
    public void testRunFinished(Result result) {
        for (final MongodProcess process : processes) {
            process.stop();
        }
    }

}
