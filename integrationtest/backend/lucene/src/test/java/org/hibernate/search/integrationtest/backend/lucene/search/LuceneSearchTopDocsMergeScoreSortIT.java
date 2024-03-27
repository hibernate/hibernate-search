/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;

/**
 * Test that one can use {@link org.apache.lucene.search.TopDocs#merge(int, TopDocs[])}
 * to merge top docs coming from different Lucene search queries
 * (which could run on different server nodes),
 * when relying on score sort.
 * <p>
 * This is a use case in Infinispan, in particular.
 */

class LuceneSearchTopDocsMergeScoreSortIT {

	private static final String SEGMENT_0 = "seg0";
	private static final String SEGMENT_1 = "seg1";

	private static final String SEGMENT_0_DOC_0 = "0_0";
	private static final String SEGMENT_0_DOC_1 = "0_1";
	private static final String SEGMENT_0_DOC_NON_MATCHING = "0_nonMatching";
	private static final String SEGMENT_1_DOC_0 = "1_0";
	private static final String SEGMENT_1_DOC_1 = "1_1";
	private static final String SEGMENT_1_DOC_NON_MATCHING = "1_nonMatching";

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	void desc() {
		LuceneSearchQuery<DocumentReference> segment0Query = matchTextSortedByScoreQuery( SortOrder.DESC, SEGMENT_0 );
		LuceneSearchQuery<DocumentReference> segment1Query = matchTextSortedByScoreQuery( SortOrder.DESC, SEGMENT_1 );
		LuceneSearchResult<DocumentReference> segment0Result = segment0Query.fetch( 10 );
		LuceneSearchResult<DocumentReference> segment1Result = segment1Query.fetch( 10 );
		assertThatResult( segment0Result )
				.hasDocRefHitsExactOrder( index.typeName(), SEGMENT_0_DOC_0, SEGMENT_0_DOC_1 );
		assertThatResult( segment1Result )
				.hasDocRefHitsExactOrder( index.typeName(), SEGMENT_1_DOC_0, SEGMENT_1_DOC_1 );

		TopFieldDocs[] allTopDocs = retrieveTopDocs( segment0Query, segment0Result, segment1Result );
		assertThat( TopDocs.merge( 10, allTopDocs ).scoreDocs )
				.containsExactly(
						allTopDocs[1].scoreDocs[0], // SEGMENT_1_DOC_0
						allTopDocs[0].scoreDocs[0], // SEGMENT_0_DOC_0
						allTopDocs[1].scoreDocs[1], // SEGMENT_1_DOC_1
						allTopDocs[0].scoreDocs[1] // SEGMENT_0_DOC_0
				);
	}

	// Also check ascending order:
	// 1. to be sure the above didn't just pass by chance;
	// 2. because the TopDocs merging method is not the same in that case.
	@Test
	void asc() {
		LuceneSearchQuery<DocumentReference> segment0Query = matchTextSortedByScoreQuery( SortOrder.ASC, SEGMENT_0 );
		LuceneSearchQuery<DocumentReference> segment1Query = matchTextSortedByScoreQuery( SortOrder.ASC, SEGMENT_1 );
		LuceneSearchResult<DocumentReference> segment0Result = segment0Query.fetch( 10 );
		LuceneSearchResult<DocumentReference> segment1Result = segment1Query.fetch( 10 );
		assertThatResult( segment0Result )
				.hasDocRefHitsExactOrder( index.typeName(), SEGMENT_0_DOC_1, SEGMENT_0_DOC_0 );
		assertThatResult( segment1Result )
				.hasDocRefHitsExactOrder( index.typeName(), SEGMENT_1_DOC_1, SEGMENT_1_DOC_0 );

		TopFieldDocs[] allTopDocs = retrieveTopDocs( segment0Query, segment0Result, segment1Result );
		assertThat( TopDocs.merge( segment0Query.luceneSort(), 10, allTopDocs ).scoreDocs )
				.containsExactly(
						allTopDocs[0].scoreDocs[0], // SEGMENT_0_DOC_1
						allTopDocs[1].scoreDocs[0], // SEGMENT_1_DOC_1
						allTopDocs[0].scoreDocs[1], // SEGMENT_0_DOC_0
						allTopDocs[1].scoreDocs[1] // SEGMENT_1_DOC_0
				);
	}

	private LuceneSearchQuery<DocumentReference> matchTextSortedByScoreQuery(SortOrder sortOrder, String routingKey) {
		StubMappingScope scope = index.createScope();
		return scope.query().extension( LuceneExtension.get() )
				.where( f -> f.match().field( "text" ).matching( "hooray" ) )
				.sort( f -> f.score().order( sortOrder ) )
				.routing( routingKey )
				.toQuery();
	}

	@SafeVarargs // while the method doesn't need to be final anymore with JDK 11 it still requires a SafeVarargs to make Eclipse compiler happy
	private TopFieldDocs[] retrieveTopDocs(LuceneSearchQuery<?> query, LuceneSearchResult<DocumentReference>... results) {
		Sort sort = query.luceneSort();
		TopFieldDocs[] allTopDocs = new TopFieldDocs[results.length];
		for ( int i = 0; i < results.length; i++ ) {
			TopDocs topDocs = results[i].topDocs();
			allTopDocs[i] = new TopFieldDocs( topDocs.totalHits, topDocs.scoreDocs, sort.getSort() );
		}
		return allTopDocs;
	}

	private static void initData() {
		index.bulkIndexer()
				// Important: do not index the documents in the expected order after sorts
				.add( SEGMENT_0_DOC_1, SEGMENT_0, document -> {
					document.addValue( index.binding().text, "Hooray" );
				} )
				.add( SEGMENT_0_DOC_0, SEGMENT_0, document -> {
					document.addValue( index.binding().text, "Hooray Hooray Hooray" );
				} )
				.add( SEGMENT_0_DOC_NON_MATCHING, SEGMENT_0, document -> {
					document.addValue( index.binding().text, "No match" );
				} )
				.add( SEGMENT_1_DOC_0, SEGMENT_1, document -> {
					document.addValue( index.binding().text, "Hooray Hooray Hooray Hooray" );
				} )
				.add( SEGMENT_1_DOC_1, SEGMENT_1, document -> {
					document.addValue( index.binding().text, "Hooray Hooray" );
				} )
				.add( SEGMENT_1_DOC_NON_MATCHING, SEGMENT_1, document -> {
					document.addValue( index.binding().text, "No match" );
				} )
				.join();
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
