/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;

/**
 * Test that one can use {@link TopDocs#totalHits}
 * on the {@link LuceneSearchResult#topDocs() topDocs returned by Lucene search queries}
 * and get the correct total hit count,
 * even when Hibernate Search relies on internal optimizations to compute the total hit count through other means.
 * <p>
 * This is a use case in Infinispan, in particular.
 */

class LuceneSearchTopDocsTotalHitCountOnMatchAllDocsIT {

	private static final int DOCUMENT_COUNT = 2000;

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( DOCUMENT_COUNT, i -> StubMapperUtils.documentProvider(
						String.valueOf( i ),
						document -> {
							document.addValue( index.binding().text, "Hooray" );
						} ) )
				.join();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4068") // The regression was spotted early, while introducing it in HSEARCH-4068
	void matchAllDocs_sortByScoreDesc() {
		LuceneSearchResult<DocumentReference> result = index.query().extension( LuceneExtension.get() )
				.where( f -> f.matchAll() )
				.fetch( 10 );

		assertThatResult( result ).hasTotalHitCount( DOCUMENT_COUNT );

		TopDocs topDocs = result.topDocs();
		assertThat( topDocs.totalHits.relation ).isEqualTo( TotalHits.Relation.EQUAL_TO );
		assertThat( topDocs.totalHits.value ).isEqualTo( DOCUMENT_COUNT );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> text;

		IndexBinding(IndexSchemaElement root) {
			text = root.field(
					"text",
					f -> f.asString()
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
		}
	}
}
