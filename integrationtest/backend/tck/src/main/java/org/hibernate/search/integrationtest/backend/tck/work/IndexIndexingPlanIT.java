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

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubBackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubEntityReference;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;

@RunWith(Parameterized.class)
public class IndexIndexingPlanIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
	public final SearchSetupHelper setupHelper;

	private final StubBackendSessionContext sessionContext;

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	public IndexIndexingPlanIT(String label, Function<TckBackendHelper, TckBackendSetupStrategy> setupStrategyFunction,
			StubBackendSessionContext sessionContext) {
		this.setupHelper = new SearchSetupHelper( setupStrategyFunction );
		this.sessionContext = sessionContext;
	}

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void success() {
		IndexIndexingPlan<?> plan = index.createIndexingPlan( sessionContext );

		// Add
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().title, "The Lord of the Rings chap. 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( index.binding().title, "The Lord of the Rings chap. 2" ) );
		plan.add( referenceProvider( "3" ), document -> document.addValue( index.binding().title, "The Lord of the Rings chap. 3" ) );
		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		SearchResultAssert.assertThat( index.createScope().query( sessionContext )
				.where( f -> f.match().field( "title" ).matching( "Lord" ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "3" );

		// Update
		plan.update( referenceProvider( "2" ), document -> document.addValue( index.binding().title, "The Boss of the Rings chap. 2" ) );
		future = plan.execute();
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		SearchResultAssert.assertThat( index.createScope().query( sessionContext )
				.where( f -> f.match().field( "title" ).matching( "Lord" ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "3" );

		// Delete
		plan.delete( referenceProvider( "1" ) );
		future = plan.execute();
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		SearchResultAssert.assertThat( index.createScope().query( sessionContext )
				.where( f -> f.match().field( "title" ).matching( "Lord" ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), "3" );
	}

	@Test
	public void discard() {
		IndexIndexingPlan<?> plan = index.createIndexingPlan( sessionContext );

		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().title, "Title of Book 1" ) );
		plan.discard();
		plan.add( referenceProvider( "2" ), document -> document.addValue( index.binding().title, "Title of Book 2" ) );

		CompletableFuture<?> future = plan.execute();
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		SearchResultAssert.assertThat( index.createScope().query( sessionContext )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), "2" );
	}

	@Test
	public void add_failure() {
		IndexIndexingPlan<?> plan = index.createIndexingPlan( sessionContext );
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().title, "Title of Book 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( index.binding().title, "Title of Book 2" ) );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexingOperationsFail( index.name() );

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
	public void update_failure() {
		setup();

		IndexIndexingPlan<?> plan = index.createIndexingPlan( sessionContext );
		plan.update( referenceProvider( "1" ), document -> document.addValue( index.binding().title, "Title of Book 1" ) );
		plan.update( referenceProvider( "2" ), document -> document.addValue( index.binding().title, "Title of Book 2" ) );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexingOperationsFail( index.name() );

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
	public void delete_failure() {
		setup();

		IndexIndexingPlan<?> plan = index.createIndexingPlan( sessionContext );
		plan.delete( referenceProvider( "1" ) );
		plan.delete( referenceProvider( "2" ) );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexingOperationsFail( index.name() );

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

		IndexIndexingPlan<StubEntityReference> plan = index.createIndexingPlan( sessionContext );
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().title, "Title of Book 1" ) );
		plan.add( referenceProvider( "2" ), document -> document.addValue( index.binding().title, "Title of Book 2" ) );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexingOperationsFail( index.name() );

		CompletableFuture<IndexIndexingPlanExecutionReport<StubEntityReference>> future = plan.executeAndReport();
		Awaitility.await().until( future::isDone );

		// The operation should succeed, but the report should indicate a failure.
		FutureAssert.assertThat( future ).isSuccessful( report -> {
			assertThat( report ).isNotNull();
			SoftAssertions.assertSoftly( softly -> {
				softly.assertThat( report.throwable() ).containsInstanceOf( SearchException.class );
				softly.assertThat( report.failingEntityReferences() )
						.containsExactly(
								new StubEntityReference( index.typeName(), "1" ),
								new StubEntityReference( index.typeName(), "2" )
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

	private static class IndexBinding {
		final IndexFieldReference<String> title;

		IndexBinding(IndexSchemaElement root) {
			title = root.field(
					"title",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
		}
	}
}
