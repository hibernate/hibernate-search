/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.classloading;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Search;

/**
 * @author Hardy Ferentschik
 */
public class NoAnnotationsTest extends org.hibernate.search.test.TestCase {

	/**
	 * Tests that @DocumentId is optional. See HSEARCH-104.
	 *
	 * @throws Exception in case the test fails.
	 */
	public void testConfigurationWithoutAnnotations() throws Exception {
		Animal dog = new Animal();
		dog.setName( "Dog" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( dog );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		List results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "name", "dog" ) )
		).list();
		assertEquals( 1, results.size() );
		tx.commit();
		s.close();
	}
	
	public void testFlushListenerRegistrationWithoutAnnotations() throws Exception {
		// This test should pass even if the flushListener is not registered,
		// as a workaround is done in code (you'll see a warning in logs).
		Animal pinguin = new Animal();
		pinguin.setName( "Penguin" );

		Session s = openSession();
		s.save( pinguin );
		s.flush();
		s.clear();

		Transaction tx = s.beginTransaction();
		tx = s.beginTransaction();
		List results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "name", "penguin" ) )
		).list();
		assertEquals( 1, results.size() );
		tx.commit();
		s.close();
	}

	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/search/test/classloading/Animal.hbm.xml"
		};
	}
}
