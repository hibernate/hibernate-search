/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.reader;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.awaitility.Awaitility;

class LuceneIndexReaderRefreshBaseIT {

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
	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( DocumentCommitStrategy.NONE, null ),
				Arguments.of( DocumentCommitStrategy.FORCE, null ),
				Arguments.of( DocumentCommitStrategy.NONE, 0 ),
				Arguments.of( DocumentCommitStrategy.FORCE, 0 ),
				Arguments.of( DocumentCommitStrategy.NONE, NON_ZERO_DELAY ),
				Arguments.of( DocumentCommitStrategy.FORCE, NON_ZERO_DELAY )
		);
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@ParameterizedTest(name = "Commit strategy {0}, commit_interval {1}")
	@MethodSource("params")
	void ioStrategyDefault_refreshIntervalDefault(DocumentCommitStrategy commitStrategy, Integer commitInterval) {
		setup( null, null, commitInterval );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				commitStrategy, // This is irrelevant
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasTotalHitCount( 1 );
	}

	@ParameterizedTest(name = "Commit strategy {0}, commit_interval {1}")
	@MethodSource("params")
	void ioStrategyDefault_refreshIntervalZero(DocumentCommitStrategy commitStrategy, Integer commitInterval) {
		setup( null, 0, commitInterval );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				commitStrategy, // This is irrelevant
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasTotalHitCount( 1 );
	}

	@ParameterizedTest(name = "Commit strategy {0}, commit_interval {1}")
	@MethodSource("params")
	void ioStrategyDefault_refreshIntervalPositive_refreshStrategyNone(DocumentCommitStrategy commitStrategy,
			Integer commitInterval) {
		setup( null, NON_ZERO_DELAY, commitInterval );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				commitStrategy, // This is irrelevant
				DocumentRefreshStrategy.NONE // This means no refresh will take place until after the refresh interval
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Readers should *not* be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasNoHits();

		// ... but they should be after some time
		Awaitility.await().untilAsserted( () -> assertThatQuery( query ).hasTotalHitCount( 1 ) );
	}

	@ParameterizedTest(name = "Commit strategy {0}, commit_interval {1}")
	@MethodSource("params")
	void ioStrategyDefault_refreshIntervalPositive_refreshStrategyForce(DocumentCommitStrategy commitStrategy,
			Integer commitInterval) {
		setup( null, NON_ZERO_DELAY, commitInterval );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				commitStrategy, // This is irrelevant
				DocumentRefreshStrategy.FORCE // This will force a refresh before the end of the refresh interval
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasTotalHitCount( 1 );
	}

	@ParameterizedTest(name = "Commit strategy {0}, commit_interval {1}")
	@MethodSource("params")
	void ioStrategyDebug(DocumentCommitStrategy commitStrategy, Integer commitInterval) {
		setup( "debug", null, commitInterval );

		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.FORCE, // With the debug IO strategy, commit is necessary for changes to be visible
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( index.binding().textField, "text1" ) );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThatQuery( query ).hasTotalHitCount( 1 );
	}

	private void setup(String ioStrategyName, Integer refreshIntervalMs, Integer commitInterval) {
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
