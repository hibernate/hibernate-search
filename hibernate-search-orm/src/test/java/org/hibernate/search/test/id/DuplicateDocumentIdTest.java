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
package org.hibernate.search.test.id;

import org.hibernate.search.SearchException;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Hardy Ferentschik
 */
public class DuplicateDocumentIdTest extends SearchTestCase {

	public void setUp() {
		// don't call super.setUp - we want to initialize the SessionFactory in the test
		buildConfiguration();
	}

	/**
	 * Tests that an exception is thrown in case @DocumentId is specified on more than one property
	 *
	 * @throws Exception in case the test fails.
	 */
	public void testDuplicateDocumentId() throws Exception {
		try {
			openSessionFactory();
			fail( "Building the SessionFactory should fail, because Foo defines multiple document ids." );
		}
		catch ( SearchException e ) { // getting a HibernateException here, because the listener registration fails
			assertEquals(
					"HSEARCH000167: More than one @DocumentId specified on entity 'org.hibernate.search.test.id.Foo'",
					e.getMessage()
			);
		}
	}

	@Override
	protected void closeSessionFactory() {
		// don't fail because it can't close the SessionFactory: that's expected
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Foo.class
		};
	}
}
