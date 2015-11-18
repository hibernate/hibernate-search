/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.testutil;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Helper for running specific tests from the "orm" module in the context of the ES backend module. Adapt the classname
 * filter as needed to run a single test or an entire test package.
 *
 * @author Gunnar Morling
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({ ".*FieldAccessTest" })
// @ClasspathSuite.ClassnameFilters({ "org.hibernate.search.test.*" })
public class OrmTestRunner {
}
