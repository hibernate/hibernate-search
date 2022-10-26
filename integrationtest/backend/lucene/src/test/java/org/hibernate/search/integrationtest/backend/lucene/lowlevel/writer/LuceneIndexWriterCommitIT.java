/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.awaitility.Awaitility;

@RunWith(Parameterized.class)
public class LuceneIndexWriterCommitIT {

	/*
	 * Pick a value that is:
	 * - large enough that background indexing threads actually wait before committing
	 * - small enough that Awaitility.await does not give up before this number of milliseconds
	 * - small enough that tests do not take forever to execute
	 */
	private static final int NON_ZERO_DELAY = 1000;

	@Parameterized.Parameters(name = "I/O strategy {0}, commit_interval {1}")
	public static Object[][] strategies() {
		return new Object[][] {
				{ "debug", null },
				{ null, null },
				{ null, 0 },
				{ null, NON_ZERO_DELAY },
				{ "near-real-time", null },
				{ "near-real-time", 0 },
				{ "near-real-time", NON_ZERO_DELAY }
		};
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final String ioStrategyName;
	private final Integer commitInterval;

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	public LuceneIndexWriterCommitIT(String ioStrategyName, Integer commitInterval) {
		this.ioStrategyName = ioStrategyName;
		this.commitInterval = commitInterval;
	}

	@Test
	public void commitStrategyNone() throws IOException {
		setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP );

		// Initially our document is not in the index
		assertThat( countDocsOnDisk() ).isEqualTo( 0 );

		// Add the document to the index
		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.NONE, // The commit will happen at some point, but the indexing plan will be considered completed before that
				DocumentRefreshStrategy.NONE // This is irrelevant
		);
		plan.add( referenceProvider( "1" ), document -> { } );
		plan.execute( OperationSubmitter.BLOCKING ).join();

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

	@Test
	public void commitStrategyForce() throws IOException {
		setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP );

		// Initially our document is not in the index
		assertThat( countDocsOnDisk() ).isEqualTo( 0 );

		// Add the document to the index
		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.FORCE, // The commit will happen before the indexing plan is considered completed
				DocumentRefreshStrategy.NONE // This is irrelevant
		);
		plan.add( referenceProvider( "1" ), document -> { } );
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// Commit should have happened before indexing finished
		assertThat( countDocsOnDisk() ).isEqualTo( 1 );
	}

	/**
	 * Test that changes are actually committed when closing the integration.
	 */
	@Test
	public void integrationClose() throws IOException {
		StubMapping mapping = setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY );

		// Initially our document is not in the index
		assertThat( countDocsOnDisk() ).isEqualTo( 0 );

		// Add the document to the index
		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.NONE, // The commit should not be necessary for changes to be visible
				DocumentRefreshStrategy.NONE // The refresh should be done regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> { } );
		plan.execute( OperationSubmitter.BLOCKING ).join();

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

	private StubMapping setup(StubMappingSchemaManagementStrategy schemaManagementStrategy) {
		return setupHelper.start()
				.withSchemaManagement( schemaManagementStrategy )
				.withIndex( index )
				.withBackendProperty( LuceneIndexSettings.IO_STRATEGY, ioStrategyName )
				.withBackendProperty( LuceneIndexSettings.IO_COMMIT_INTERVAL, commitInterval )
				.setup();
	}
}
