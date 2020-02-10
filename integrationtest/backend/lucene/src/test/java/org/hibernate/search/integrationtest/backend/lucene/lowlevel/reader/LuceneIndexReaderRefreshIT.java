/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.reader;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubBackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;

public class LuceneIndexReaderRefreshIT {

	private static final String INDEX_NAME = "IndexName";
	/*
	 * Pick a value that is:
	 * - large enough that test code executes faster than this number of milliseconds, even on slow machines
	 * - small enough that Awaitility.await does not give up before this number of milliseconds
	 * - small enough that tests do not take forever to execute
	 */
	private static final int NON_ZERO_DELAY = 2000;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Test
	public void ioStrategyDefault_refreshIntervalDefault() {
		setup( null, null );

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThat( query ).hasNoHits();

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan(
				new StubBackendSessionContext(),
				DocumentCommitStrategy.NONE, // The commit should not be necessary for changes to be visible
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.textField, "text1" ) );
		plan.execute().join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThat( query ).hasTotalHitCount( 1 );
	}

	@Test
	public void ioStrategyDefault_refreshIntervalZero() {
		setup( null, 0 );

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThat( query ).hasNoHits();

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan(
				new StubBackendSessionContext(),
				DocumentCommitStrategy.NONE, // The commit should not be necessary for changes to be visible
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.textField, "text1" ) );
		plan.execute().join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThat( query ).hasTotalHitCount( 1 );
	}

	@Test
	public void ioStrategyDefault_refreshIntervalPositive_refreshStrategyNone() {
		setup( null, NON_ZERO_DELAY );

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThat( query ).hasNoHits();

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan(
				new StubBackendSessionContext(),
				DocumentCommitStrategy.NONE, // The commit should not be necessary for changes to be visible
				DocumentRefreshStrategy.NONE // This means no refresh will take place until after the refresh interval
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.textField, "text1" ) );
		plan.execute().join();

		// Readers should *not* be up-to-date immediately after indexing finishes
		assertThat( query ).hasNoHits();

		// ... but they should be after some time
		Awaitility.await().untilAsserted( () -> assertThat( query ).hasTotalHitCount( 1 ) );
	}

	@Test
	public void ioStrategyDefault_refreshIntervalPositive_refreshStrategyForce() {
		setup( null, NON_ZERO_DELAY );

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThat( query ).hasNoHits();

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan(
				new StubBackendSessionContext(),
				DocumentCommitStrategy.NONE, // The commit should not be necessary for changes to be visible
				DocumentRefreshStrategy.FORCE // This will force a refresh before the end of the refresh interval
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.textField, "text1" ) );
		plan.execute().join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThat( query ).hasTotalHitCount( 1 );
	}

	@Test
	public void ioStrategyDebug() {
		setup( "debug", null );

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.where( f -> f.match().field( "text" ).matching( "text1" ) )
				.toQuery();

		assertThat( query ).hasNoHits();

		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan(
				new StubBackendSessionContext(),
				DocumentCommitStrategy.NONE, // The commit should not be necessary for changes to be visible
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.textField, "text1" ) );
		plan.execute().join();

		// Readers should be up-to-date immediately after indexing finishes
		assertThat( query ).hasTotalHitCount( 1 );
	}

	private void setup(String ioStrategyName, Integer refreshIntervalMs) {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndexDefaultsProperty( LuceneIndexSettings.IO_STRATEGY, ioStrategyName )
				.withIndexDefaultsProperty( LuceneIndexSettings.IO_REFRESH_INTERVAL, refreshIntervalMs )
				.setup();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> textField;

		IndexMapping(IndexSchemaElement root) {
			textField = root.field( "text", c -> c.asString() ).toReference();
		}
	}
}
