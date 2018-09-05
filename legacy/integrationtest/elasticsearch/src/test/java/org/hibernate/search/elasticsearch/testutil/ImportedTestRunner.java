/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Helper for running specific tests from the "orm" or "engine" module in the context
 * of the Elasticsearch module.
 *
 * <p>The main difference with running tests directly is that those tests will use
 * configuration files from the Elasticsearch module (see
 * <code>src/test/resources/hibernate.properties</code> and
 * <code>src/test/resources/searchfactoryholder-defaults.properties</code>).
 *
 *<p>Adapt the classname filter as needed to run a single test or an entire test package.
 *
 * <p>Running such tests from the IDE will require having an Elasticsearch
 * node running. You can easily start one by running 'mvn elasticsearch:runforked'
 * from the elasticsearch module in this project.
 *
 * @author Gunnar Morling
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({ ".*ProjectionQueryTest" })
// @ClasspathSuite.ClassnameFilters({ "org.hibernate.search.test.*" })
public class ImportedTestRunner {
}
