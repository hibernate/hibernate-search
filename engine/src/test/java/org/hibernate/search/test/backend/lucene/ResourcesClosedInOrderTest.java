/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.BytemanHelperStateCleanup;
import org.hibernate.search.testsupport.setup.CountingErrorHandler;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.search.testsupport.BytemanHelper.isEventStackEmpty;
import static org.hibernate.search.testsupport.BytemanHelper.consumeNextRecordedEvent;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(BMUnitRunner.class)
@BMRules(rules = {
		@BMRule(
				name = "trackIndexWriterCommit",
				targetClass = "org.apache.lucene.index.IndexWriter",
				targetMethod = "commit()",
				helper = "org.hibernate.search.testsupport.BytemanHelper",
				action = "pushEvent(\"commit\")"
			),
		@BMRule(
				name = "trackIndexWriterClose",
				targetClass = "org.apache.lucene.index.IndexWriter",
				targetMethod = "close()",
				helper = "org.hibernate.search.testsupport.BytemanHelper",
				action = "pushEvent(\"close\")"
			),
		@BMRule(
				name = "trackUpdatesBeingApplied",
				targetClass = "org.hibernate.search.backend.impl.lucene.LuceneBackendQueueTask",
				targetMethod = "applyUpdates()",
				helper = "org.hibernate.search.testsupport.BytemanHelper",
				action = "pushEvent(\"applyUpdates\")"
			),
		} )
/**
 * Verifies each Lucene backend is flushing and closing the IndexWriter as expected
 * by its configuration, especially flags such as 'exclusive_index_use' and synchronous
 * vs asynchronous configurations.
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
public class ResourcesClosedInOrderTest {

	private static final int NUMBER_ENTITIES = 2;

	@Rule
	public BytemanHelperStateCleanup bytemanState = new BytemanHelperStateCleanup();

	@Test
	public void asyncExclusiveIndexResourcesOrderedShutdown() {
		//The first "close" event is caused by index initialisation at boot.
		//Then, we expect apply operations to be executed without any close nor commit.
		expectOnConfiguration( true, true, "close", "applyUpdates", "applyUpdates", "close" );
	}

	@Test
	public void asyncSharedIndexResourcesOrderedShutdown() {
		//The first "close" event is caused by index initialisation at boot.
		//But as the index is 'shared' the IndexWriter shall be closed after each write.
		expectOnConfiguration( true, false, "close", "applyUpdates", "close", "applyUpdates", "close" );
	}

	@Test
	public void synchExclusiveIndexResourcesOrderedShutdown() {
		//The first "close" event is caused by index initialisation at boot.
		//But as the indexing is synchronous (yet not using NRT), a commit is required after each write.
		expectOnConfiguration( false, true, "close", "applyUpdates", "commit", "applyUpdates", "commit", "close" );
	}

	@Test
	public void synchSharedIndexResourcesOrderedShutdown() {
		//The first "close" event is caused by index initialisation at boot.
		//But as the indexing is synchronous and shared, a 'close' is required after each write.
		expectOnConfiguration( false, false, "close", "applyUpdates", "close", "applyUpdates", "close" );
	}

	private void expectOnConfiguration(boolean async, boolean exclusiveIndexing, String... expectedStack) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addProperty( "hibernate.search.default.worker.execution", async ? "async" : "sync" );
		cfg.addProperty( "hibernate.search.default.exclusive_index_use", exclusiveIndexing ? "true" : "false" );
		cfg.addProperty( "hibernate.search.error_handler", CountingErrorHandler.class.getName() );
		cfg.addClass( Quote.class );
		try ( SearchIntegrator searchIntegrator = new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator() ) {
			final CountingErrorHandler errorHandler = (CountingErrorHandler) searchIntegrator.getErrorHandler();
			writeData( searchIntegrator, NUMBER_ENTITIES );
			//Check no errors happened in the asynchronous threads
			assertEquals( 0, errorHandler.getTotalCount() );
		}

		//Now the SearchIntegrator was closed, let's unwind the recorder events and compare them with expectations:
		for ( int i = 0; i < expectedStack.length; i++ ) {
			//Check the events have been fired in the expected order
			assertEquals( expectedStack[i], consumeNextRecordedEvent() );
		}
		//And no more events than those expected exist
		assertTrue( isEventStackEmpty() );
	}

	private void writeData(SearchIntegrator searchIntegrator, int numberEntities) {
		int entityIdGenerator = 0;
		for ( int i = 0; i < numberEntities; i++ ) {
			Integer id = Integer.valueOf( entityIdGenerator++ );
			Quote quote = new Quote( id, "description" );
			Work work = new Work( quote, id, WorkType.ADD, false );
			TransactionContextForTest tc = new TransactionContextForTest();
			searchIntegrator.getWorker().performWork( work, tc );
			tc.end();
		}
	}

}
