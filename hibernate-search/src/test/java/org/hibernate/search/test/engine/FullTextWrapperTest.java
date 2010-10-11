package org.hibernate.search.test.engine;

import org.hibernate.search.Search;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class FullTextWrapperTest extends SearchTestCase {

	public void testSearch() throws Exception {
		try {
			Search.getFullTextSession( null );
		}
		catch (IllegalArgumentException e) {
			//good
		}

		try {
			org.hibernate.search.jpa.Search.getFullTextEntityManager( null );
		}
		catch (IllegalArgumentException e) {
			//good
		}
	}

	// Test setup options - Entities
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BusLine.class, BusStop.class };
	}

	// Test setup options - SessionFactory Properties
	@Override
	protected void configure(org.hibernate.cfg.Configuration configuration) {
		super.configure( configuration );
		cfg.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
	}
}
