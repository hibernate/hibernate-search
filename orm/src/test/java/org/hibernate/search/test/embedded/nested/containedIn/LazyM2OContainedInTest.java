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

package org.hibernate.search.test.embedded.nested.containedIn;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.testing.cache.CachingRegionFactory;

/**
 * @author Emmanuel Bernard
 */
public class LazyM2OContainedInTest extends SearchTestCase {

	//HSEARCH-385
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

		assertEquals( 1, fts.createFullTextQuery( new TermQuery( new Term("uid", new Long(uid1).toString() ) ), Entity1ForDoc0.class ).getResultSize() );
		assertEquals( 1, fts.createFullTextQuery( new TermQuery( new Term("entities2.uid", String.valueOf( uid2 ) ) ), Entity1ForDoc0.class ).getResultSize() );


		tx.commit();

		tx = fts.beginTransaction();
		for ( Entity2ForDoc0 e : (List<Entity2ForDoc0>) fts.createCriteria( Entity2ForDoc0.class ).list() ) {
			fts.delete( e );
		}
		for ( Entity1ForDoc0 e : (List<Entity1ForDoc0>) fts.createCriteria( Entity1ForDoc0.class ).list() ) {
			fts.delete( e );
		}
		tx.commit();
	}

	//HSEARCH-386
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
				.createFullTextQuery( new TermQuery( new Term( "entity1.uid", new Long( ent1_0.getUid() ).toString() ) ), Entity2ForUnindexed.class )
				.getResultSize() );
		Entity1ForUnindexed toDelete = (Entity1ForUnindexed) fts.get( Entity1ForUnindexed.class, otherId );

		fts.delete( toDelete );

		fts.getTransaction().begin();
		fts.getTransaction().commit();
		fts.clear();

		assertEquals( 0, fts.createFullTextQuery( new TermQuery( new Term("entity1.uid", String.valueOf( otherId ) ) ), Entity2ForUnindexed.class ).getResultSize() );

		tx = fts.beginTransaction();
		for ( Entity2ForUnindexed e : (List<Entity2ForUnindexed>) fts.createCriteria( Entity2ForUnindexed.class ).list() ) {
			fts.delete( e );
		}
		for ( Entity1ForUnindexed e : (List<Entity1ForUnindexed>) fts.createCriteria( Entity1ForUnindexed.class ).list() ) {
			fts.delete( e );
		}
		tx.commit();

		fts.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Entity1ForDoc0.class,
				Entity2ForDoc0.class,
				Entity1ForUnindexed.class,
				Entity2ForUnindexed.class
		};
	}
}
