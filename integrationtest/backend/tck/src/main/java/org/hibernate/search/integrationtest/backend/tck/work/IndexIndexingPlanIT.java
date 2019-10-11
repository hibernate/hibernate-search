/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;
import org.awaitility.Awaitility;

public class IndexIndexingPlanIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String INDEX_NAME = "indexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Test
	public void success() {
		setup( null );

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );

		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();

		SearchResultAssert.assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" );
	}

	@Test
	public void discard() {
		setup( null );

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();

		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.discard();
		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );

		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();

		SearchResultAssert.assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2" );
	}

	@Test
	public void failure_defaultHandler() {
		setup( null );

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

		// The default failure handler should log at the ERROR level at least once.
		// The ES backend may report multiple failures because it executes all works in bulk.
		logged.expectLevel( Level.ERROR );

		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		// TODO HSEARCH-3736 Exceptions are not propagated to the future when using the Elasticsearch backend
		//FutureAssert.assertThat( future ).isFailed();

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
		}
	}

	@Test
	public void failure_customHandler() {
		setup( StubFailureHandler.class.getName() );

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

		// The default failure handler should not receive any failure
		logged.expectLevel( Level.ERROR ).never();

		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		// TODO HSEARCH-3736 Exceptions are not propagated to the future when using the Elasticsearch backend
		//FutureAssert.assertThat( future ).isFailed();

		// The custom failure handler should have received a failure.
		// The ES backend may report multiple failures because it executes all works in bulk.
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isGreaterThanOrEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
		}
	}

	private void setup(String failureHandler) {
		setupHelper.start()
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, failureHandler )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> title;

		IndexMapping(IndexSchemaElement root) {
			title = root.field( "name", f -> f.asString() ).toReference();
		}
	}
}
