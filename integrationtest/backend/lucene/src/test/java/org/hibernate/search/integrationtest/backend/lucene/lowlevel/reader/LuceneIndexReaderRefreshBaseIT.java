/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.reader;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.awaitility.Awaitility;

@RunWith(Parameterized.class)
public class LuceneIndexReaderRefreshBaseIT {

	/*
	 * Pick a value that is:
	 * - large enough that test code executes faster than this number of milliseconds, even on slow machines
	 * - small enough that Awaitility.await does not give up before this number of milliseconds
	 * - small enough that tests do not take forever to execute
	 */
	private static final int NON_ZERO_DELAY = 2000;

	/**
	 * These parameters should not have any effect on the test.
	 * They are here to check that we get the same behavior independently from these parameters.
	 */
	@Parameterized.Parameters(name = "Commit strategy {0}, commit_interval {1}")
	public static Object[][] strategies() {
		return new Object[][] {
				{ DocumentCommitStrategy.NONE, null },
				{ DocumentCommitStrategy.FORCE, null },
				{ DocumentCommitStrategy.NONE, 0 },
				{ DocumentCommitStrategy.FORCE, 0 },
				{ DocumentCommitStrategy.NONE, NON_ZERO_DELAY },
				{ DocumentCommitStrategy.FORCE, NON_ZERO_DELAY }
		};
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private final DocumentCommitStrategy commitStrategy;
	private final Integer commitInterval;

	public LuceneIndexReaderRefreshBaseIT(DocumentCommitStrategy commitStrategy, Integer commitInterval) {
		this.commitStrategy = commitStrategy;
		this.commitInterval = commitInterval;
	}

	@Test
	public void ioStrategyDefault_refreshIntervalDefault() {
		setup( null, null );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				commitStrategy, // This is irrelevant
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasTotalHitCount( 1 );
	}

	@Test
	public void ioStrategyDefault_refreshIntervalZero() {
		setup( null, 0 );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				commitStrategy, // This is irrelevant
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasTotalHitCount( 1 );
	}

	@Test
	public void ioStrategyDefault_refreshIntervalPositive_refreshStrategyNone() {
		setup( null, NON_ZERO_DELAY );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				commitStrategy, // This is irrelevant
				DocumentRefreshStrategy.NONE // This means no refresh will take place until after the refresh interval
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// Readers should *not* be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasNoHits();

		// ... but they should be after some time
		Awaitility.await().untilAsserted( () -> assertThatQuery( query ).hasTotalHitCount( 1 ) );
	}

	@Test
	public void ioStrategyDefault_refreshIntervalPositive_refreshStrategyForce() {
		setup( null, NON_ZERO_DELAY );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				commitStrategy, // This is irrelevant
				DocumentRefreshStrategy.FORCE // This will force a refresh before the end of the refresh interval
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasTotalHitCount( 1 );
	}

	@Test
	public void ioStrategyDebug() {
		setup( "debug", null );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.FORCE, // With the debug IO strategy, commit is necessary for changes to be visible
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasTotalHitCount( 1 );
	}

	private void setup(String ioStrategyName, Integer refreshIntervalMs) {
		setupHelper.start()
				.withIndex( index )
				.withBackendProperty( LuceneIndexSettings.IO_STRATEGY, ioStrategyName )
				.withBackendProperty( LuceneIndexSettings.IO_REFRESH_INTERVAL, refreshIntervalMs )
				.withBackendProperty( LuceneIndexSettings.IO_COMMIT_INTERVAL, commitInterval )
				.setup();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> textField;

		IndexBinding(IndexSchemaElement root) {
			textField = root.field( "text", c -> c.asString() ).toReference();
		}
	}
}
