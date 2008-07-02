//$Id$
package org.hibernate.search.test.bridge;

import org.hibernate.search.SearchException;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.cfg.AnnotationConfiguration;
import junit.framework.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class UnresolvedBridgeTest extends TestCase {
	public void testSerializableType() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();

		for (int i = 0; i < getMappings().length; i++) {
			cfg.addAnnotatedClass( getMappings()[i] );
		}
		cfg.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		try {
			cfg.buildSessionFactory();
			fail("Undefined bridge went through");
		}
		catch( Exception e ) {
			Throwable ee = e;
			boolean hasSearchException = false;
			for (;;) {
				if (ee == null) {
					break;
				}
				else if (ee instanceof SearchException) {
					hasSearchException = true;
					break;
				}
				ee = ee.getCause();
			}
			assertTrue( hasSearchException );
		}
	}

	@SuppressWarnings("unchecked")
	protected Class[] getMappings() {
		return new Class[] {
				Gangster.class
		};
	}
}
