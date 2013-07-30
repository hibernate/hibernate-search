/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.backend;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.store.optimization.impl.IncrementalOptimizerStrategy;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class OptimizationTriggerTest extends SearchTestCaseJUnit4 {

	@Test
	public void testOptimizationIsTriggered() throws InterruptedException {
		DirectoryBasedIndexManager indexManager = getSingleIndexManager( Clock.class );

		OptimizerStrategy optimizerStrategy = indexManager.getOptimizerStrategy();
		Assert.assertTrue( "Unexpected optimizer strategy", optimizerStrategy instanceof IncrementalOptimizerStrategy );

		// let's start the actual test
		IncrementalOptimizerStrategy strategy = (IncrementalOptimizerStrategy) optimizerStrategy;
		assertEquals( "Initially no optimisation should have been performed", 0, strategy.getOptimizationsPerformed() );

		Session session = openSession();
		//check that optimization is triggered periodically as configured
		long optimizationsPerformed = 0l;
		for ( int i = 0; i < 20; i++ ) {
			Clock c = new Clock( i, "hwd" + i );
			Transaction transaction = session.beginTransaction();
			session.persist( c );
			transaction.commit();
			session.clear();
			optimizationsPerformed = strategy.getOptimizationsPerformed();
			assertEquals(
					"Optimization should be triggered every three inserts",
					( i + 1 ) / 3,
					optimizationsPerformed
			);
		}
		session.close();

		session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Clock.class );
		int resultSize = fullTextQuery.getResultSize();
		assertEquals( "Wrong number of indexed entities", 20, resultSize );

		//an explicit invocation of #optimize() should trigger it as well
		assertEquals(
				"Optimization should not have changed",
				optimizationsPerformed,
				strategy.getOptimizationsPerformed()
		);
		fullTextSession.getSearchFactory().optimize( Clock.class );
		assertEquals(
				"Optimize should have been incremented",
				optimizationsPerformed + 1,
				strategy.getOptimizationsPerformed()
		);

		//the massIndexer should optimize only before and after (not during the process)
		fullTextSession.createIndexer( Clock.class )
				.optimizeAfterPurge( true )
				.optimizeOnFinish( true )
				.startAndWait();

		assertEquals(
				"The mass indexer should trigger optimize as well ",
				optimizationsPerformed + 3,
				strategy.getOptimizationsPerformed()
		);

		session.close();
	}

	private DirectoryBasedIndexManager getSingleIndexManager(Class<?> clazz) {
		MutableSearchFactory searchFactory = (MutableSearchFactory) getSearchFactory();
		EntityIndexBinding indexBindingForEntity = searchFactory.getIndexBinding( clazz );
		IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
		assertEquals( 1, indexManagers.length );
		return (DirectoryBasedIndexManager) indexManagers[0];
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.optimizer.operation_limit.max", "3" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}
}
