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
package org.hibernate.search.test.embedded.update;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Hardy Ferentschik
 */
public class ContainedInReindexPropagationTest extends SearchTestCase {

	// see HSEARCH-662
	public void testUpdatingContainedInEntityPropagatesToAllEntities() throws Exception {
		// source to move dads from
		Grandpa source = new Grandpa( "grandpaSource" );
		// source to move dads to
		Grandpa target = new Grandpa( "grandpaTarget" );

		Dad dad1 = new Dad( "dad1" );
		dad1.setGrandpa( source );

		Dad dad2 = new Dad( "dad2" );
		dad2.setGrandpa( source );

		Son son1 = new Son( "son1" );
		dad1.add( son1 );

		Son son2 = new Son( "son2" );
		dad2.add( son2 );

		// first operation -> save
		FullTextSession session = Search.getFullTextSession( openSession() );
		Transaction tx = session.beginTransaction();
		session.save( source );
		session.save( target );
		session.save( dad1 );
		session.save( dad2 );
		session.save( son1 );
		session.save( son2 );
		tx.commit();
		session.close();

		final Long sourceGrandpaId = source.getId();
		final Long targetGrandpaId = target.getId();

		// assert that everything got saved correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();

		// everything gets indexed correctly
		assertEquals( getSonsGrandpaIdFromIndex( session, son1.getName() ), sourceGrandpaId );
		assertEquals( getSonsGrandpaIdFromIndex( session, son2.getName() ), sourceGrandpaId );

		tx.commit();
		session.close();

		//let's move dad's to a new grandpa!
		dad1.setGrandpa( target );
		dad2.setGrandpa( target );

		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();

		session.update( dad1 );
		session.update( dad2 );

		tx.commit();
		session.close();

		//all right, let's assert that indexes got updated correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();

		//NOTE: it looks like the last item in session (i.e. dad2) does get updated correctly
		assertEquals( "must now point target!", getSonsGrandpaIdFromIndex( session, son2.getName() ), targetGrandpaId );
		//this one will fail miserably
		assertEquals( "must now point target!", getSonsGrandpaIdFromIndex( session, son1.getName() ), targetGrandpaId );

		tx.commit();
		session.close();
	}

	private Long getSonsGrandpaIdFromIndex(FullTextSession session, String name) {
		FullTextQuery q = session.createFullTextQuery( new TermQuery( new Term( "name", name ) ), Son.class );
		q.setProjection( "dad.grandpaId" );
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.list();
		if ( results.isEmpty() ) {
			return null;
		}
		return (Long) results.get( 0 )[0];
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Dad.class, Grandpa.class, Son.class
		};
	}
}
