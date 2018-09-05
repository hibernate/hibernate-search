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

import static org.junit.Assert.assertEquals;


/**
 * Verifies the global setting from {@link org.hibernate.search.cfg.spi.SearchConfiguration#isIndexMetadataComplete()}
 * affect the backends as expected.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
@Category(SkipOnElasticsearch.class) // The DeleteByTerm matter is specific to Lucene
public class IndexMetadataCompleteConfiguredTest extends BaseConfigurationTest {

	@Test
	public void testDefaultImplementation() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		verifyIndexCompleteMetadataOption( true, cfg );
	}

	@Test
	public void testIndexMetadataCompleteFalse() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setIndexMetadataComplete( false );
		verifyIndexCompleteMetadataOption( false, cfg );
	}

	@Test
	public void testIndexMetadataCompleteTrue() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setIndexMetadataComplete( true );
		verifyIndexCompleteMetadataOption( true, cfg );
	}

	private void verifyIndexCompleteMetadataOption(boolean expectation, SearchConfigurationForTest cfg) {
		MutableSearchFactory sf = getMutableSearchFactoryWithSingleEntity( cfg );
		try {
			assertEquals( expectation, extractWorkspace( sf, Document.class ).areSingleTermDeletesSafe() );

			// trigger a SearchFactory rebuild:
			sf.addClasses( Dvd.class, Book.class );
			// DVD share the same index, so now it's always unsafe [always false no matter the global option]
			assertEquals( false, extractWorkspace( sf, Dvd.class ).areSingleTermDeletesSafe() );
			assertEquals( false, extractWorkspace( sf, Document.class ).areSingleTermDeletesSafe() );

			// but still as expected for Book :
			assertEquals( expectation, extractWorkspace( sf, Book.class ).areSingleTermDeletesSafe() );
		}
		finally {
			sf.close();
		}
	}

}
