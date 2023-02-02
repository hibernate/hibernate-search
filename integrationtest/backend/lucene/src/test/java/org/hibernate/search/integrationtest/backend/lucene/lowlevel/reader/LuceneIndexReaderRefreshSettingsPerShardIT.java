/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.reader;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.lucene.sharding.AbstractSettingsPerShardIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

import org.awaitility.Awaitility;

@TestForIssue(jiraKey = "HSEARCH-3636")
public class LuceneIndexReaderRefreshSettingsPerShardIT extends AbstractSettingsPerShardIT {

	/*
	 * Pick a value that is:
	 * - large enough that test code executes faster than this number of milliseconds, even on slow machines
	 * - small enough that Awaitility.await does not give up before this number of milliseconds
	 * - small enough that tests do not take forever to execute
	 */
	private static final int NON_ZERO_DELAY = 2000;

	public LuceneIndexReaderRefreshSettingsPerShardIT(String ignoredLabel, SearchSetupHelper setupHelper, List<String> shardIds) {
		super( ignoredLabel, setupHelper, shardIds );
	}

	@Test
	public void test() {
		setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), "io.refresh_interval", NON_ZERO_DELAY )
				.withIndexProperty( index.name(), "shards." + shardIds.get( 2 ) + ".io.refresh_interval", 0 )
				.setup();

		SearchQuery<DocumentReference> query = index.createScope().query().where( f -> f.matchAll() ).toQuery();

		// Initially the shards are empty
		assertThatQuery( query ).hasNoHits();

		IndexIndexingPlan plan = index.createIndexingPlan(
				DocumentCommitStrategy.NONE, // This is irrelevant
				DocumentRefreshStrategy.NONE // The refresh should be executed regardless of this parameter
		);
		for ( int i = 0; i < 400; i++ ) {
			plan.add( referenceProvider( String.valueOf( i ), routingKey( i ) ), document -> { } );
		}
		plan.execute( OperationSubmitter.blocking() ).join();

		// Readers should be up-to-date immediately after indexing finishes for shard 2
		// but not (yet) for shards 0, 1 and 3,
		assertThatQuery( query ).totalHitCount().isBetween( 1L, 199L );

		// ... but they should be after some time
		Awaitility.await().untilAsserted( () -> assertThatQuery( query ).totalHitCount().isEqualTo( 400L ) );
	}

}
