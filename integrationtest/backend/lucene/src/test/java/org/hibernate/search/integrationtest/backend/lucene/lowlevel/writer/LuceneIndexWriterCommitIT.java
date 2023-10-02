/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.awaitility.Awaitility;

class LuceneIndexWriterCommitIT {

	/*
	 * Pick a value that is:
	 * - large enough that background indexing threads actually wait before committing
	 * - small enough that Awaitility.await does not give up before this number of milliseconds
	 * - small enough that tests do not take forever to execute
	 */
	private static final int NON_ZERO_DELAY = 1000;

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( "debug", null ),
				Arguments.of( null, null ),
				Arguments.of( null, 0 ),
				Arguments.of( null, NON_ZERO_DELAY ),
				Arguments.of( "near-real-time", null ),
				Arguments.of( "near-real-time", 0 ),
				Arguments.of( "near-real-time", NON_ZERO_DELAY )
		);
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@ParameterizedTest(name = "I/O strategy {0}, commit_interval {1}")
	@MethodSource("params")
	void commitStrategyNone(String ioStrategyName, Integer commitInterval) throws IOException {
		setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP, ioStrategyName, commitInterval );

		// Initially our document is not in the index
		assertThat( countDocsOnDisk() ).isEqualTo( 0 );

		// Add the document to the index
		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.NONE, // The commit will happen at some point, but the indexing plan will be considered completed before that
				DocumentRefreshStrategy.NONE // This is irrelevant
		);
		plan.add( referenceProvider( "1" ), document -> {} );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Commit will happen some time after indexing finished
		Awaitility.await().untilAsserted( () -> {
			try {
				assertThat( countDocsOnDisk() ).isEqualTo( 1 );
			}
			catch (IOException e) {
				// May happen if we call the method *right* as a commit is executing
				fail( "countDocsOnDisk() failed: " + e.getMessage(), e );
			}
		} );
	}

	@ParameterizedTest(name = "I/O strategy {0}, commit_interval {1}")
	@MethodSource("params")
	void commitStrategyForce(String ioStrategyName, Integer commitInterval) throws IOException {
		setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP, ioStrategyName, commitInterval );

		// Initially our document is not in the index
		assertThat( countDocsOnDisk() ).isEqualTo( 0 );

		// Add the document to the index
		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.FORCE, // The commit will happen before the indexing plan is considered completed
				DocumentRefreshStrategy.NONE // This is irrelevant
		);
		plan.add( referenceProvider( "1" ), document -> {} );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Commit should have happened before indexing finished
		assertThat( countDocsOnDisk() ).isEqualTo( 1 );
	}

	/**
	 * Test that changes are actually committed when closing the integration.
	 */
	@ParameterizedTest(name = "I/O strategy {0}, commit_interval {1}")
	@MethodSource("params")
	void integrationClose(String ioStrategyName, Integer commitInterval) throws IOException {
		StubMapping mapping = setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY,
				ioStrategyName,
				commitInterval
		);

		// Initially our document is not in the index
		assertThat( countDocsOnDisk() ).isEqualTo( 0 );

		// Add the document to the index
		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.NONE, // The commit should not be necessary for changes to be visible
				DocumentRefreshStrategy.NONE // The refresh should be done regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> {} );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Stop Hibernate Search
		mapping.close();

		// Commit may have happened at any time, but it must be done by the time integration.close() returns
		assertThat( countDocsOnDisk() ).isEqualTo( 1 );
	}

	/**
	 * @return The number of document that are actually present in the low-level, physical representation of the index.
	 * This bypasses Hibernate Search, its index writer and its index readers,
	 * so only committed changes will be taken into account.
	 * @throws IOException If an I/O failure occurs.
	 */
	private int countDocsOnDisk() throws IOException {
		return LuceneIndexContentUtils.readIndex(
				setupHelper, index.name(),
				reader -> reader.getDocCount( MetadataFields.idFieldName() )
		);
	}

	private StubMapping setup(StubMappingSchemaManagementStrategy schemaManagementStrategy, String ioStrategyName,
			Integer commitInterval) {
		return setupHelper.start()
				.withSchemaManagement( schemaManagementStrategy )
				.withIndex( index )
				.withBackendProperty( LuceneIndexSettings.IO_STRATEGY, ioStrategyName )
				.withBackendProperty( LuceneIndexSettings.IO_COMMIT_INTERVAL, commitInterval )
				.setup();
	}
}
