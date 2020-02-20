/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * A basic test for disabled sharding with explicit routing keys.
 */
public class ShardingDisabledRoutingKeyIT extends AbstractShardingIT {

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper(
			TckBackendHelper::createNoShardingBackendSetupStrategy
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
		// Use more than one routing key
		int routingKeyCount = 10;
		int documentCountPerRoutingKey = 100;
		int totalDocumentCount = routingKeyCount * documentCountPerRoutingKey;

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
		Iterator<String> iterator = docIdByRoutingKey.keySet().iterator();
		String someRoutingKey = iterator.next();
		String someOtherRoutingKey = iterator.next();

		/*
		 * One routing key => all documents indexed with that routing key should be returned,
		 * and only those documents.
		 */
		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.where( f -> f.matchAll() )
						.routing( someRoutingKey )
						.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( documentCountPerRoutingKey )
				.containsExactlyInAnyOrder( docRefsForRoutingKey( someRoutingKey, docIdByRoutingKey ) );

		if ( !TckConfiguration.get().getBackendFeatures().supportsManyRoutingKeys() ) {
			return;
		}

		/*
		 * Two routing keys => all documents indexed with these routing keys should be returned,
		 * and only those documents.
		 */
		List<String> twoRoutingKeys = Arrays.asList( someRoutingKey, someOtherRoutingKey );
		SearchResultAssert.assertThat(
				indexManager.createScope().query()
						.where( f -> f.matchAll() )
						.routing( twoRoutingKeys )
						.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( documentCountPerRoutingKey * 2 )
				.containsExactlyInAnyOrder( docRefsForRoutingKeys( twoRoutingKeys, docIdByRoutingKey ) );

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
