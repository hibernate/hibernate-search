/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import org.hibernate.search.engine.impl.MutableSearchFactory;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;

/**
 * Test to force deleteByTerm on backend
 *
 * @author gustavonalle
 */
@Category(SkipOnElasticsearch.class) // The DeleteByTerm matter is specific to Lucene
public class DeleteByTermEnforcementTest extends BaseConfigurationTest {

	@Test
	public void testDefaultValue() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setDeleteByTermEnforced( true );

		verifyDeleteByTermEnforced( cfg );
	}

	private void verifyDeleteByTermEnforced(SearchConfigurationForTest cfg) {
		try (MutableSearchFactory sf = getMutableSearchFactoryWithSingleEntity( cfg )) {
			assertTrue( extractWorkspace( sf, Document.class ).isDeleteByTermEnforced() );

			// trigger a SearchFactory rebuild:
			sf.addClasses( Dvd.class, Book.class );

			// single terms should always be marked as safe since we are forcing it
			assertTrue( extractWorkspace( sf, Book.class ).isDeleteByTermEnforced() );
			assertTrue( extractWorkspace( sf, Dvd.class ).isDeleteByTermEnforced() );
			assertTrue( extractWorkspace( sf, Document.class ).isDeleteByTermEnforced() );
		}
	}
}
