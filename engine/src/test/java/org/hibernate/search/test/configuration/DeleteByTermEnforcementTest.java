/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import org.hibernate.search.engine.impl.MutableSearchFactory;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test to force deleteByTerm on backend
 *
 * @author gustavonalle
 */
public class DeleteByTermEnforcementTest extends BaseConfigurationTest {

	@Test
	public void testEnforcement() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setDeleteByTermEnforced( true );

		verifyDeleteByTerm( true, cfg );
	}

	@Test
	public void testDefaults() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();

		verifyDeleteByTerm( false, cfg );
	}

	@Test
	public void testWithMetadataComplete() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setIndexMetadataComplete( true );

		verifyDeleteByTerm( false, cfg );
	}

	private void verifyDeleteByTerm(boolean enforced, SearchConfigurationForTest cfg) {
		try (MutableSearchFactory sf = getMutableSearchFactoryWithSingleEntity( cfg )) {
			assertEquals( enforced, extractWorkspace( sf, Document.class ).isDeleteByTermEnforced() );

			// trigger a SearchFactory rebuild:
			sf.addClasses( Dvd.class, Book.class );

			assertEquals( enforced, extractWorkspace( sf, Book.class ).isDeleteByTermEnforced() );
			assertEquals( enforced, extractWorkspace( sf, Dvd.class ).isDeleteByTermEnforced() );
			assertEquals( enforced, extractWorkspace( sf, Document.class ).isDeleteByTermEnforced() );
		}
	}
}
