/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Helper for running specific TCK tests against the Elasticsearch backend from the IDE.
 * <p>
 * Adapt the {@code @IncludeClassNamePatterns}/{@code @SelectPackages} annotation as needed
 * to run a single test or an entire test package.
 * <p>
 * Elasticsearch will be started automatically using TestContainers.
 *
 * @author Gunnar Morling
 */
@Suite
@SuiteDisplayName("Elasticsearch TCK tests Runner")
// Defines a "root" package, subpackages are included. Use Include/Exclude ClassNamePatterns annotations to limit the executed tests:
@SelectPackages("org.hibernate.search.integrationtest.backend.tck")
// Default class pattern does not include IT tests, hence we want to customize it a bit:
@IncludeClassNamePatterns({ ".*Test", ".*IT" })
public class ElasticsearchTckTestRunner {
}
