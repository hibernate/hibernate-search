/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubBackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubEntityReference;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;

@RunWith(Parameterized.class)
public class IndexIndexingPlanIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String TYPE_NAME = "typeName";
	private static final String INDEX_NAME = "indexName";

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		return new Object[][] {
				{
						"No multi-tenancy",
						(Function<TckBackendHelper, TckBackendSetupStrategy>) TckBackendHelper::createDefaultBackendSetupStrategy,
						new StubBackendSessionContext()
				},
				{
						"Multi-tenancy enabled explicitly",
						(Function<TckBackendHelper, TckBackendSetupStrategy>) TckBackendHelper::createMultiTenancyBackendSetupStrategy,
						new StubBackendSessionContext( "tenant_1" )
				}
		};
	}

	@Rule
	public SearchSetupHelper setupHelper;

	private final StubBackendSessionContext sessionContext;

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	public IndexIndexingPlanIT(String label, Function<TckBackendHelper, TckBackendSetupStrategy> setupStrategyFunction,
			StubBackendSessionContext sessionContext) {
		this.setupHelper = new SearchSetupHelper( setupStrategyFunction );
		this.sessionContext = sessionContext;
	}

	@Test
	public void success() {
		setup();

		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan( sessionContext );
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );

		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		SearchQuery<DocumentReference> query = indexManager.createScope().query( sessionContext )
				.where( f -> f.matchAll() )
				.toQuery();

		SearchResultAssert.assertThat( query )
				.hasDocRefHitsAnyOrder( TYPE_NAME, "1", "2" );
	}

	@Test
	public void discard() {
		setup();

		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan( sessionContext );

		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.discard();
		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );

		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		SearchQuery<DocumentReference> query = indexManager.createScope().query( sessionContext )
				.where( f -> f.matchAll() )
				.toQuery();

		SearchResultAssert.assertThat( query )
				.hasDocRefHitsAnyOrder( TYPE_NAME, "2" );
	}

	@Test
	public void failure() {
		setup();

		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan( sessionContext );
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3852")
	public void failure_report() {
		setup();

		IndexIndexingPlan<StubEntityReference> plan = indexManager.createIndexingPlan( sessionContext );
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> future = plan.executeAndReport();
		Awaitility.await().until( future::isDone );

		// The operation should succeed, but the report should indicate a failure.
		FutureAssert.assertThat( future ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.getThrowable() ).containsInstanceOf( SearchException.class );
				softly.assertThat( report.getFailingEntityReferences() )
						.containsExactly(
								new StubEntityReference( TYPE_NAME, "1" ),
								new StubEntityReference( TYPE_NAME, "2" )
						);
			} );
		} );

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
						options -> options.mappedType( TYPE_NAME ),
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
