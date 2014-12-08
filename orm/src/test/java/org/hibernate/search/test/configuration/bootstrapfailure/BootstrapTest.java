/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.bootstrapfailure;

import java.util.Set;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1447")
public class BootstrapTest extends SearchTestBase {

	@Test
	public void testCreateIndexSearchEntityWithLobField() {
		Set<Class<?>> indexedTypes = getSearchFactory().getIndexedTypes();

		assertTrue( "There should only be one indexed entity", indexedTypes.size() == 1 );
		assertTrue(
				"Unexpected indexed type: " + getSearchFactory().getIndexedTypes(),
				getSearchFactory().getIndexedTypes().contains( IndexedEntity.class )
		);

		assertNull(
				"NoSearchEntity should not have a DocumentBuilderContainedEntity",
				getExtendedSearchIntegrator()
				.unwrap( ExtendedSearchIntegrator.class )
				.getDocumentBuilderContainedEntity( NoSearchEntity.class )
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				// just adding NoSearchEntity causes an exception, even though it is not used from a Search perspective
				IndexedEntity.class, EmbeddedEntity.class, NoSearchEntity.class
		};
	}
}


