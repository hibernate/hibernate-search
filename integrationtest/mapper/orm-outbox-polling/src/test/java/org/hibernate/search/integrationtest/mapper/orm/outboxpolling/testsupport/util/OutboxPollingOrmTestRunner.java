/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Helper for running specific Hibernate ORM integration tests with outbox-polling from the IDE.
 * <p>
 * Adapt the {@code @IncludeClassNamePatterns}/{@code @SelectPackages} annotation as needed
 * to run a single test or an entire test package.
 * <p>
 * If tests against a non-H2 database are requested (e.g. with the ci-postgressql Maven profile),
 * the database will be started automatically using TestContainers.
 */
@Suite
@SuiteDisplayName("Outbox polling tests Runner")
// Defines a "root" package, subpackages are included. Use Include/Exclude ClassNamePatterns annotations to limit the executed tests:
@SelectPackages("org.hibernate.search.integrationtest.mapper.orm.automaticindexing")
// Default class pattern does not include IT tests, hence we want to customize it a bit:
@IncludeClassNamePatterns({ ".*Test", ".*IT" })
public class OutboxPollingOrmTestRunner {
}
