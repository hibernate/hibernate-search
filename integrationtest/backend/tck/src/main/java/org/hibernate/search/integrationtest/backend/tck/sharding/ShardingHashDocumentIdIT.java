/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * A basic test for hash-based sharding with document IDs (without routing keys).
 */
@PortedFromSearch5(original = "org.hibernate.search.test.shards.ShardsTest")
class ShardingHashDocumentIdIT extends AbstractShardingIT {

	private static final int SHARD_COUNT = 3;
	// Create more document than shards, so that multiple documents end up in the same shard, like in real life
	private static final int ESTIMATED_DOCUMENT_COUNT_PER_SHARD = 100;
	private static final int TOTAL_DOCUMENT_COUNT = SHARD_COUNT * ESTIMATED_DOCUMENT_COUNT_PER_SHARD;

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final Map<String, List<String>> docIdByRoutingKey = new HashMap<>();
	private final List<String> docIds = new ArrayList<>();

	public ShardingHashDocumentIdIT() {
		super( RoutingMode.DOCUMENT_IDS );
	}

	@BeforeEach
	void setup() {
		setupHelper.start( tckBackendHelper -> tckBackendHelper.createHashBasedShardingBackendSetupStrategy( SHARD_COUNT ) )
				.withIndex( index ).setup();

		// Do not provide explicit routing keys when indexing; the backend should fall back to using IDs
		for ( int documentIdAsInteger = 0; documentIdAsInteger < TOTAL_DOCUMENT_COUNT;
				documentIdAsInteger++ ) {
			// Just make sure document IDs are unique across all routing keys
			String documentId = "someText_" + documentIdAsInteger;
			docIds.add( documentId );
		}

		docIdByRoutingKey.put( null, docIds );
		initData( docIdByRoutingKey );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3314")
	void search() {
		// No routing key => all documents should be returned
		assertThatQuery( index.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( TOTAL_DOCUMENT_COUNT )
				.containsExactlyInAnyOrder( allDocRefs( docIdByRoutingKey ) );

		Iterator<String> iterator = docIds.iterator();
		String someDocumentId = iterator.next();
		String someOtherDocumentId = iterator.next();

		// One or more explicit routing key => no document should be returned, since no documents was indexed with that routing key.
		assertThatQuery(
				index.createScope().query()
						.where( f -> f.matchAll() )
						.routing( someDocumentId )
						.toQuery()
		)
				.hasNoHits();

		// Multiple explicit routing keys => no result: documents were indexed without a routing key.
		assertThatQuery(
				index.createScope().query()
						.where( f -> f.matchAll() )
						.routing( Arrays.asList( someDocumentId, someOtherDocumentId ) )
						.toQuery()
		)
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3824")
	void purgeWithRoutingKey() {
		assumePurgeSupported();

		Iterator<String> iterator = docIds.iterator();
		String someDocumentId = iterator.next();

		index.createWorkspace()
				.purge( Collections.singleton( someDocumentId ), OperationSubmitter.blocking(),
						UnsupportedOperationBehavior.FAIL )
				.join();

		// One or more explicit routing key => no document should be purged, since no documents was indexed with that routing key.
		index.searchAfterIndexChanges( () -> assertThatQuery( index.query().where( f -> f.matchAll() ) )
				.hits().asNormalizedDocRefs()
				.hasSize( TOTAL_DOCUMENT_COUNT )
				.containsExactlyInAnyOrder( allDocRefs( docIdByRoutingKey ) ) );
	}

	private void assumePurgeSupported() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsExplicitPurge(),
				"This test only makes sense if the backend supports explicit purge"
		);
	}

}
