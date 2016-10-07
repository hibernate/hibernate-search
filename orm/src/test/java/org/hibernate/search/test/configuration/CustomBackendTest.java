/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.backend.impl.LocalBackendQueueProcessor;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessor;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

/**
 * @author Sanne Grinovero
 */
@Category(SkipOnElasticsearch.class) // Worker backends are specific to the Lucene backend
public class CustomBackendTest {

	@Test
	public void test() {
		verifyBackendUsage( "blackhole", BlackHoleBackendQueueProcessor.class );
		verifyBackendUsage( "local", LocalBackendQueueProcessor.class );
		verifyBackendUsage( "lucene", LocalBackendQueueProcessor.class );
		verifyBackendUsage( BlackHoleBackendQueueProcessor.class );
		verifyBackendUsage( LocalBackendQueueProcessor.class );
	}

	private void verifyBackendUsage(String name, Class<? extends BackendQueueProcessor> backendType) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		FullTextSession ftSession = builder
			.setProperty( "hibernate.search.default.worker.backend", name )
			.addAnnotatedClass( BlogEntry.class )
			.openFullTextSession();
		ExtendedSearchIntegrator integrator = ftSession.getSearchFactory().unwrap( ExtendedSearchIntegrator.class );
		ftSession.close();
		IndexManagerHolder allIndexesManager = integrator.getIndexManagerHolder();
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) allIndexesManager.getIndexManager( "org.hibernate.search.test.configuration.BlogEntry" );
		BackendQueueProcessor backendQueueProcessor = allIndexesManager.getBackendQueueProcessor( indexManager.getIndexName() );
		assertEquals( backendType, backendQueueProcessor.getClass() );
		builder.close();
	}

	public void verifyBackendUsage(Class<? extends BackendQueueProcessor> backendType) {
		verifyBackendUsage( backendType.getName(), backendType );
	}

}
