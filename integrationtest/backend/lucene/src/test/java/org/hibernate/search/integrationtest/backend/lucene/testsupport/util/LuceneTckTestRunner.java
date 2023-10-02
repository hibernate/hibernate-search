/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Helper for running specific TCK tests against the Lucene backend from the IDE.
 * <p>
 * Adapt the {@code @IncludeClassNamePatterns}/{@code @SelectPackages} annotation as needed
 * to run a single test or an entire test package.
 *
 * @author Gunnar Morling
 */
@Suite
@SuiteDisplayName("Lucene TCK tests Runner")
// Defines a "root" package, subpackages are included. Use Include/Exclude ClassNamePatterns annotations to limit the executed tests:
@SelectPackages("org.hibernate.search.integrationtest.backend.tck")
// Default class pattern does not include IT tests, hence we want to customize it a bit:
@IncludeClassNamePatterns({ ".*Test", ".*IT" })
public class LuceneTckTestRunner {

}
