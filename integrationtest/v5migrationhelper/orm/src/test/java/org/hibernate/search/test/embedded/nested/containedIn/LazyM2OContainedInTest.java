/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.nested.containedIn;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.hibernate.testing.cache.CachingRegionFactory;

import org.junit.Test;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author Emmanuel Bernard
 */
public class LazyM2OContainedInTest extends SearchTestBase {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-385")
	@SuppressWarnings("unchecked")
	public void testDocumentsAt0() {
		FullTextSession fts = Search.getFullTextSession( getSessionFactory().openSession() );
		Transaction tx = fts.beginTransaction();
		final Entity1ForDoc0 ent1 = new Entity1ForDoc0();
		final Entity2ForDoc0 ent2 = new Entity2ForDoc0();

		ent2.setEntity1( ent1 );
		ent1.getEntities2().add( ent2 );

		ent2.setName( "test - 1" );

		fts.persist( ent1 );
		fts.persist( ent2 );

		tx.commit();

		long uid2 = ent2.getUid();
		long uid1 = ent1.getUid();


		fts.clear();

		tx = fts.beginTransaction();

		assertEquals(
				1,
				fts.createFullTextQuery( LongPoint.newExactQuery( "uid", uid1 ), Entity1ForDoc0.class ).getResultSize()
		);
		assertEquals(
				1,
				fts.createFullTextQuery( LongPoint.newExactQuery( "entities2.uid", uid2 ), Entity1ForDoc0.class )
						.getResultSize()
		);

		tx.commit();

		tx = fts.beginTransaction();
		for ( Entity2ForDoc0 e : listAll( fts, Entity2ForDoc0.class ) ) {
			fts.delete( e );
		}
		for ( Entity1ForDoc0 e : listAll( fts, Entity1ForDoc0.class ) ) {
			fts.delete( e );
		}
		tx.commit();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-386")
	@SuppressWarnings("unchecked")
	public void testContainedInAndLazy() {
		FullTextSession fts = Search.getFullTextSession( getSessionFactory().openSession() );
		Entity1ForUnindexed ent1_0 = new Entity1ForUnindexed();
		Entity1ForUnindexed ent1_1 = new Entity1ForUnindexed();

		Entity2ForUnindexed ent2_0 = new Entity2ForUnindexed();
		Entity2ForUnindexed ent2_1 = new Entity2ForUnindexed();

		ent2_0.setEntity1( ent1_0 );
		ent1_0.getEntities2().add( ent2_0 );

		ent2_1.setEntity1( ent1_1 );
		ent1_1.getEntities2().add( ent2_1 );

		// persist outside the tx
		fts.persist( ent1_0 );
		fts.persist( ent1_1 );
		fts.persist( ent2_0 );
		fts.persist( ent2_1 );

		Transaction tx = fts.beginTransaction();
		tx.commit(); //flush

		fts.clear();

		Entity1ForUnindexed other = new Entity1ForUnindexed();
		fts.persist( other );

		fts.getTransaction().begin();
		fts.getTransaction().commit();
		fts.clear();

		//FIXME that's not guaranteed to happen before flush
		long otherId = other.getUid();

		assertEquals( 1, fts
				.createFullTextQuery(
						LongPoint.newExactQuery( "entity1.uid-numeric", ent1_0.getUid() ),
						Entity2ForUnindexed.class
				)
				.getResultSize() );
		Entity1ForUnindexed toDelete = (Entity1ForUnindexed) fts.get( Entity1ForUnindexed.class, otherId );

		fts.delete( toDelete );

		fts.getTransaction().begin();
		fts.getTransaction().commit();
		fts.clear();

		assertEquals( 0,
				fts.createFullTextQuery( new TermQuery( new Term( "entity1.uid", String.valueOf( otherId ) ) ),
						Entity2ForUnindexed.class ).getResultSize() );

		tx = fts.beginTransaction();
		for ( Entity2ForUnindexed e : listAll( fts, Entity2ForUnindexed.class ) ) {
			fts.delete( e );
		}
		for ( Entity1ForUnindexed e : listAll( fts, Entity1ForUnindexed.class ) ) {
			fts.delete( e );
		}
		tx.commit();

		fts.close();
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Entity1ForDoc0.class,
				Entity2ForDoc0.class,
				Entity1ForUnindexed.class,
				Entity2ForUnindexed.class
		};
	}
}
