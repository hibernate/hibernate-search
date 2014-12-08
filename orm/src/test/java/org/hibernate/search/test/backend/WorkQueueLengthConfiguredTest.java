/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.backend;

import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the is <code>max_queue_length</code> parameter for Lucene backend is read.
 * (see HSEARCH-520)
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class WorkQueueLengthConfiguredTest extends SearchTestBase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	@Test
	public void testNothingTest() {
		SearchIntegrator searchFactory = getSearchFactory().unwrap( SearchIntegrator.class );
		EntityIndexBinding indexBindingForEntity = searchFactory.getIndexBinding( Clock.class );
		IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
		assertEquals( 1, indexManagers.length );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers[0];
		LuceneBackendQueueProcessor backend = (LuceneBackendQueueProcessor) indexManager.getBackendQueueProcessor();
		assertEquals( 5, backend.getIndexResources().getMaxQueueLength() );
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.max_queue_length", "5" );
	}

}
