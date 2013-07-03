/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.bridge;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.SearchException;
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
		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
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
				else if (ee instanceof SearchException) {
					hasSearchException = true;
					break;
				}
				ee = ee.getCause();
			}
			assertTrue( hasSearchException );
		}
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Gangster.class
		};
	}
}
