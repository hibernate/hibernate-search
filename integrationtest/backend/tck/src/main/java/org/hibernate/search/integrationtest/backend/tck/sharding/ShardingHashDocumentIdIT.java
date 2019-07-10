/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import static org.assertj.core.api.Assertions.withinPercentage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.NormalizedDocRefHit;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * A basic test for hash-based sharding with document IDs (without routing keys).
 */
@PortedFromSearch5(original = "org.hibernate.search.test.shards.ShardsTest")
public class ShardingHashDocumentIdIT extends AbstractShardingIT {

	private static final int SHARD_COUNT = 3;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper(
			tckBackendHelper -> tckBackendHelper.createHashBasedShardingBackendSetupStrategy( SHARD_COUNT )
	);

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx, RoutingMode.DOCUMENT_IDS ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3314")
	public void test() {
		// Use more routing keys than shards, so that multiple routing keys lead to the same shard, like in real life
		int estimatedDocumentCountPerShard = 100;
		int totalDocumentCount = SHARD_COUNT * estimatedDocumentCountPerShard;

		// Do not provide explicit routing keys when indexing; the backend should fall back to using IDs
		List<String> docIds = new ArrayList<>();
		for ( int documentIdAsInteger = 0; documentIdAsInteger < totalDocumentCount;
				documentIdAsInteger++ ) {
			// Just make sure document IDs are unique across all routing keys
			String documentId = "someText_" + documentIdAsInteger;
			docIds.add( documentId );
		}

		Map<String, List<String>> docIdByRoutingKey = new HashMap<>();
		docIdByRoutingKey.put( null, docIds );
		initData( docIdByRoutingKey );

		// No routing key => all documents should be returned
		SearchResultAssert.assertThat( indexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( totalDocumentCount )
				.containsExactlyInAnyOrder( allDocRefs( docIdByRoutingKey ) );

		// Now test with a specific document ID as routing key
		String someDocumentId = docIds.iterator().next();

		SearchResultAssert<DocumentReference> documentIdAsRoutingKeyQueryAssert = SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.predicate( f -> f.matchAll() )
						.routing( someDocumentId )
						.toQuery()
		);
		// Targeting one specific routing key, when there is a reasonable number of shards,
		// must return approximately (total number of documents) / (total number of shards).
		documentIdAsRoutingKeyQueryAssert.totalHitCount()
				.isCloseTo( estimatedDocumentCountPerShard, withinPercentage( 20 ) );
		// Check that the document ID was used as routing key
		documentIdAsRoutingKeyQueryAssert.hits().asNormalizedDocRefs()
				.contains( NormalizedDocRefHit.of( INDEX_NAME, someDocumentId ) );
	}

}
