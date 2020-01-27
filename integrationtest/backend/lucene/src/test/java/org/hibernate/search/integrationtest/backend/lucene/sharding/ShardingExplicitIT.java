/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.sharding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.integrationtest.backend.tck.sharding.AbstractShardingIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * A basic test for explicit sharding with explicit routing keys.
 */
public class ShardingExplicitIT extends AbstractShardingIT {

	private static final String BACKEND_NAME = "BackendName";

	private static final String SHARD_ID_1 = "first";
	private static final String SHARD_ID_2 = "second";
	private static final String SHARD_ID_3 = "third";
	private static final List<String> SHARD_IDS = CollectionHelper.asImmutableList(
			SHARD_ID_1, SHARD_ID_2, SHARD_ID_3
	);

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Before
	public void setup() {
		setupHelper.start( BACKEND_NAME )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx, RoutingMode.EXPLICIT_ROUTING_KEYS ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndexDefaultsProperty(
						BACKEND_NAME, LuceneIndexSettings.SHARDING_STRATEGY, "explicit"
				)
				.withIndexDefaultsProperty(
						BACKEND_NAME, LuceneIndexSettings.SHARDING_SHARD_IDENTIFIERS,
						SHARD_ID_1 + "," + SHARD_ID_2 + "," + SHARD_ID_3
				)
				.setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3314")
	public void test() {
		int documentCountPerShard = 100;
		int totalDocumentCount = SHARD_IDS.size() * documentCountPerShard;

		// Provide explicit routing keys when indexing; the routing keys are the shard IDs
		Map<String, List<String>> docIdByShardId = new HashMap<>();
		for ( String shardId : SHARD_IDS ) {
			for ( int documentIdAsIntegerForRoutingKey = 0;
					documentIdAsIntegerForRoutingKey < documentCountPerShard;
					documentIdAsIntegerForRoutingKey++ ) {
				// Just make sure document IDs are unique across all shards
				String documentId = shardId + "_" + documentIdAsIntegerForRoutingKey;
				docIdByShardId.computeIfAbsent( shardId, ignored -> new ArrayList<>() )
						.add( documentId );
			}
		}

		initData( docIdByShardId );

		// No routing key => all documents should be returned
		SearchResultAssert.assertThat( indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( totalDocumentCount )
				.containsExactlyInAnyOrder( allDocRefs( docIdByShardId ) );

		// All routing keys => all documents should be returned
		SearchResultAssert.assertThat( indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.routing( docIdByShardId.keySet() )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( totalDocumentCount )
				.containsExactlyInAnyOrder( allDocRefs( docIdByShardId ) );

		/*
		 * Specific routing key => all documents from the corresponding shards should be returned, and only those.
		 * This is the main advantage of the "explicit" sharding strategy.
		 */
		SearchResultAssert.assertThat( indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.routing( SHARD_ID_2 )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( documentCountPerShard )
				.containsExactlyInAnyOrder( docRefsForRoutingKey( SHARD_ID_2, docIdByShardId ) );
	}

}
