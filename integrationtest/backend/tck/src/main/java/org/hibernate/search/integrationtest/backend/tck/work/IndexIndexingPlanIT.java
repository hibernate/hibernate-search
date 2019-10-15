/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;

public class IndexIndexingPlanIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String INDEX_NAME = "indexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Test
	public void success() {
		setup();

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
		setup();

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
	public void failure() {
		setup();

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		FutureAssert.assertThat( future ).isFailed();

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
		}
	}

	private void setup() {
		setupHelper.start()
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
