/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.update;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author Hardy Ferentschik
 */
class ContainedInReindexPropagationTest extends SearchTestBase {

	// see HSEARCH-662
	@Test
	void testUpdatingContainedInEntityPropagatesToAllEntities() {
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
		assertThat( getSonsGrandpaIdFromIndex( session, son1.getName() ) ).isEqualTo( sourceGrandpaId );
		assertThat( getSonsGrandpaIdFromIndex( session, son2.getName() ) ).isEqualTo( sourceGrandpaId );

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
		assertThat( getSonsGrandpaIdFromIndex( session, son2.getName() ) ).as( "must now point target!" )
				.isEqualTo( targetGrandpaId );
		//this one will fail miserably
		assertThat( getSonsGrandpaIdFromIndex( session, son1.getName() ) ).as( "must now point target!" )
				.isEqualTo( targetGrandpaId );

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
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Dad.class,
				Grandpa.class,
				Son.class
		};
	}
}
