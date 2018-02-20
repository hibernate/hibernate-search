/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.util;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Helper for running specific TCK tests against the Lucene backend from the IDE.
 *
 *<p>Adapt the classpath filter as needed to run a single test or an entire test package.
 *
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({ ".*\\.tck\\..*" })
public class LuceneTckTestRunner {
}
