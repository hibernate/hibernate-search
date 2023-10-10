/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.batchindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;

import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
class CollectionInitializeTest extends SearchTestBase {

	@Test
	void testMassIndexing() throws InterruptedException {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		initializeData( fullTextSession );
		try {
			List list = listAll( fullTextSession, LegacyCarPlant.class );
			assertThat( list ).hasSize( 1 );
			fullTextSession.createIndexer( LegacyCarPlant.class ).startAndWait();
			int resultSize =
					fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), LegacyCarPlant.class ).getResultSize();
			assertThat( resultSize ).isEqualTo( 1 );
		}
		finally {
			clearData( fullTextSession );
			fullTextSession.close();
		}
	}

	private void clearData(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		fullTextSession.delete( fullTextSession.get( LegacyCarPlant.class, 12 ) );
		for ( int i = 1; i < 4; i++ ) {
			fullTextSession.delete( fullTextSession.get( LegacyCar.class, "" + i ) );
		}
		transaction.commit();
	}

	private void initializeData(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		LegacyCar[] cars = new LegacyCar[3];
		for ( int i = 1; i < 4; i++ ) {
			cars[i - 1] = new LegacyCar();
			cars[i - 1].setId( "" + i );
			cars[i - 1].setModel( "model" + i );
			fullTextSession.persist( cars[i - 1] );
		}
		LegacyCarPlant plant = new LegacyCarPlant();
		plant.setCar( cars[0] );
		plant.setName( "plant12" );
		plant.setId( 12 );
		fullTextSession.persist( plant );
		transaction.commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { LegacyCarPlant.class, LegacyCar.class, LegacyTire.class };
	}
}
