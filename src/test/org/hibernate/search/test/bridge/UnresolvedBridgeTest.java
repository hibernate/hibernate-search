//$Id$
package org.hibernate.search.test.bridge;

import java.io.InputStream;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.SearchException;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Configuration;
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
			cfg.buildSessionFactory( /*new TestInterceptor()*/ );
			fail("Undefined bridge went through");
		}
		catch( SearchException e ) {
			//success
		}
	}

	protected Class[] getMappings() {
		return new Class[] {
				Gangster.class
		};
	}
}
