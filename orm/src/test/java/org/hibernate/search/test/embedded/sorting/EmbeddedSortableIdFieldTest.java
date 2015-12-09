/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.sorting;

import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-2069")
public class EmbeddedSortableIdFieldTest extends SearchTestBase {

	@Test
	public void test() {
		// Nothing to do here: if the test fails, it will fail during initialization.
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Level1.class, Level2SortableId.class };
	}

}
