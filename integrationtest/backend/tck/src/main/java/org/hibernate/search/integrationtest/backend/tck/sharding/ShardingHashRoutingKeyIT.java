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

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * A basic test for hash-based sharding with explicit routing keys.
 */
@PortedFromSearch5(original = "org.hibernate.search.test.shards.ShardsTest")
public class ShardingHashRoutingKeyIT extends AbstractShardingIT {

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
						ctx -> this.indexMapping = new IndexMapping( ctx, RoutingMode.EXPLICIT_ROUTING_KEYS ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3314")
	public void test() {
		// Use more routing keys than shards, so that multiple routing keys lead to the same shard, like in real life
		int routingKeyCount = SHARD_COUNT * 4;
		int documentCountPerRoutingKey = 100;
		int totalDocumentCount = routingKeyCount * documentCountPerRoutingKey;
		int estimatedDocumentCountPerShard = totalDocumentCount / SHARD_COUNT;

		// Provide explicit routing keys when indexing
		Map<String, List<String>> docIdByRoutingKey = new HashMap<>();
		for ( int routingKeyAsInteger = 0; routingKeyAsInteger < routingKeyCount; routingKeyAsInteger++ ) {
			// Turn into actual text, to check that support is not limited to just numbers
			String routingKey = "someText_" + routingKeyAsInteger;
			for ( int documentIdAsIntegerForRoutingKey = 0;
					documentIdAsIntegerForRoutingKey < documentCountPerRoutingKey;
					documentIdAsIntegerForRoutingKey++ ) {
				// Just make sure document IDs are unique across all routing keys
				String documentId = routingKeyAsInteger + "_" + documentIdAsIntegerForRoutingKey;
				docIdByRoutingKey.computeIfAbsent( routingKey, ignored -> new ArrayList<>() )
						.add( documentId );
			}
		}

		initData( docIdByRoutingKey );

		// No routing key => all documents should be returned
		SearchResultAssert.assertThat( indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( totalDocumentCount )
				.containsExactlyInAnyOrder( allDocRefs( docIdByRoutingKey ) );

		// Now test with a specific routing key
		String someRoutingKey = docIdByRoutingKey.keySet().iterator().next();

		SearchResultAssert<DocumentReference> singleRoutingKeyQueryAssert = SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.where( f -> f.matchAll() )
						.routing( someRoutingKey )
						.toQuery()
		);
		/*
		 * Targeting one specific routing key, when there is a reasonable number of shards,
		 * must return approximately (total number of documents) / (total number of shards).
		 * Hash functions are not perfect, so we accept a difference of more or less 50%.
		 */
		singleRoutingKeyQueryAssert.totalHitCount()
				.isCloseTo( estimatedDocumentCountPerShard, withinPercentage( 50 ) );
		// Targeting one specific routing key must return at least all documents indexed with that routing key.
		singleRoutingKeyQueryAssert.hits().asNormalizedDocRefs()
				.contains( docRefsForRoutingKey( someRoutingKey, docIdByRoutingKey ) );

		/*
		 * In a real world scenario, users would add a predicate to the query
		 * to restrict the hits to only those documents that had this exact routing key
		 * (and not documents with different routing keys that happen to be in the same shard).
		 * Let's test this.
		 */
		SearchResultAssert.assertThat( indexManager.createScope().query()
				.where( f -> f.match().field( "indexedRoutingKey" ).matching( someRoutingKey ) )
				.routing( someRoutingKey )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.containsExactlyInAnyOrder( docRefsForRoutingKey( someRoutingKey, docIdByRoutingKey ) );

		if ( !TckConfiguration.get().getBackendFeatures().supportsManyRoutingKeys() ) {
			return;
		}

		// All routing keys => all documents should be returned
		SearchResultAssert.assertThat( indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.routing( docIdByRoutingKey.keySet() )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( totalDocumentCount )
				.containsExactlyInAnyOrder( allDocRefs( docIdByRoutingKey ) );
	}

}
