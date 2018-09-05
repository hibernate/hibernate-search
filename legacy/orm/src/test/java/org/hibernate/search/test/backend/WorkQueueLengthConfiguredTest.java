/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.backend;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.impl.lucene.WorkspaceHolder;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the is <code>max_queue_length</code> parameter for Lucene backend is read.
 * (see HSEARCH-520)
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
@Category(SkipOnElasticsearch.class) // The "max_queue_length" parameter is specific to the Lucene backend
public class WorkQueueLengthConfiguredTest extends SearchTestBase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	@Test
	public void testNothingTest() {
		SearchIntegrator searchFactory = getSearchFactory().unwrap( SearchIntegrator.class );
		EntityIndexBinding indexBindingForEntity = searchFactory.getIndexBindings().get( Clock.class );
		Set<IndexManager> indexManagers = indexBindingForEntity.getIndexManagerSelector().all();
		assertEquals( 1, indexManagers.size() );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers.iterator().next();
		WorkspaceHolder backend = (WorkspaceHolder) indexManager.getWorkspaceHolder();
		assertEquals( 5, backend.getIndexResources().getMaxQueueLength() );
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.max_queue_length", "5" );
	}

}
