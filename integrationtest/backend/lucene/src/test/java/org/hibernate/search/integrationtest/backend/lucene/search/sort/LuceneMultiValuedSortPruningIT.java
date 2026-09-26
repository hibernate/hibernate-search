/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.search.sort;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * With SortMode.SUM a document whose values sum above the current bottom may have no single value above it, so
 * pruning against the points index would drop it.
 */
@TestForIssue(jiraKey = "HSEARCH-5014")
class LuceneMultiValuedSortPruningIT {

	private static final int FILLER_COUNT = 1_000;

	// Sums to 60; neither value on its own reaches the 55 that fills the queue.
	private static final String SPLIT_ID = "split";
	private static final int SINGLE_55_COUNT = 20;

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
		initData();
	}

	@Test
	void sumMode_descending_thresholdReached() {
		List<DocumentReference> hits = index.createScope().query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "values" ).mode( SortMode.SUM ).desc() )
				.totalHitCountThreshold( 1 )
				.toQuery()
				.fetchHits( 5 );

		// 30 + 30 beats every single 55, but the points index holds 30 and 30, not 60.
		assertThat( hits ).extracting( DocumentReference::id )
				.first().isEqualTo( SPLIT_ID );
	}

	private static void initData() {
		var indexer = index.bulkIndexer();
		indexer.add( StubMapperUtils.documentProvider( SPLIT_ID, document -> {
			document.addValue( index.binding().values, 30L );
			document.addValue( index.binding().values, 30L );
		} ) );
		for ( int i = 0; i < SINGLE_55_COUNT; i++ ) {
			indexer.add( StubMapperUtils.documentProvider( "single55-" + i,
					document -> document.addValue( index.binding().values, 55L ) ) );
		}
		indexer.add( FILLER_COUNT, i -> StubMapperUtils.documentProvider( "filler-" + i,
				document -> document.addValue( index.binding().values, (long) ( i % 10 ) ) ) );
		indexer.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<Long> values;

		IndexBinding(IndexSchemaElement root) {
			values = root.field( "values", c -> c.asLong().sortable( Sortable.YES ) )
					.multiValued()
					.toReference();
		}
	}
}
