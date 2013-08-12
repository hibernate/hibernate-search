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
package org.hibernate.search.test.backend;

import java.io.File;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.junit.Assert;

public class SyncBackendLongWorkListStressTest extends SearchTestCase {

	/* needs to be sensibly higher than org.hibernate.search.batchindexing.Executors.QUEUE_MAX_LENGTH */
	private static final int NUM_SAVED_ENTITIES = 40;

	public void testWorkLongerThanMaxQueueSize() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		for ( int i = 0; i < NUM_SAVED_ENTITIES; i++ ) {
			Transaction tx = s.beginTransaction();
			Clock clock = new Clock( i, "brand num° " + i );
			s.persist( clock );
			tx.commit();
			s.clear();
		}

		Transaction tx = s.beginTransaction();
		// count of entities in database needs to be checked before SF is closed (HSQLDB will forget the entities)
		Number count = (Number) s.createCriteria( Clock.class )
				.setProjection( Projections.rowCount() )
				.uniqueResult();
		Assert.assertEquals( NUM_SAVED_ENTITIES, count.intValue() );
		tx.commit();
		s.close();

		//we need to close the SessionFactory to wait for all async work to be flushed
		closeSessionFactory();
		//and restart it again..
		openSessionFactory();

		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		int fullTextCount = s.createFullTextQuery( new MatchAllDocsQuery(), Clock.class ).getResultSize();
		Assert.assertEquals( NUM_SAVED_ENTITIES, fullTextCount );
		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
		//needs FSDirectory to have the index contents survive the SessionFactory close
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.setProperty( "hibernate.search.default.max_queue_length", "5" );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.show_sql", "false" );
		cfg.setProperty( "hibernate.format_sql", "false" );
	}

}
