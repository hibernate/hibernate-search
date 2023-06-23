/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class UnresolvedBridgeTest {

	@Test
	public void testSerializableType() throws Exception {
		Configuration cfg = new Configuration();

		for ( int i = 0; i < getAnnotatedClasses().length; i++ ) {
			cfg.addAnnotatedClass( getAnnotatedClasses()[i] );
		}
		try {
			cfg.buildSessionFactory();
			fail( "Undefined bridge went through" );
		}
		catch (Exception e) {
			Throwable ee = e;
			boolean hasSearchException = false;
			for ( ;; ) {
				if ( ee == null ) {
					break;
				}
				else if ( ee instanceof SearchException ) {
					hasSearchException = true;
					break;
				}
				ee = ee.getCause();
			}
			assertTrue( hasSearchException );
		}
	}

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Gangster.class
		};
	}
}
