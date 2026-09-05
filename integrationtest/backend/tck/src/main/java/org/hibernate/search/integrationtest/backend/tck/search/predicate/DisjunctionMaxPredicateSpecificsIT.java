/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DisjunctionMaxPredicateSpecificsIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";

	private static final String MATCHING_TERM = "search";
	private static final String OTHER_TERM = "zulu";

	// Between 1 and 2, so that the sum of the two matches of document 2 beats it, but neither match alone does
	private static final float TITLE_BOOST = 1.2f;

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	void noDisjunct() {
		assertThatQuery( index.query()
				.where( f -> f.disjunctionMax() ) )
				.hasNoHits();
	}

	@Test
	void hits_sameAsOr() {
		assertThatQuery( index.query()
				.where( f -> f.disjunctionMax()
						.add( f.match().field( "title" ).matching( MATCHING_TERM ) )
						.add( f.match().field( "description" ).matching( MATCHING_TERM ) )
						.add( f.match().field( "summary" ).matching( MATCHING_TERM ) ) ) )
				.hasTotalHitCount( 2 )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	void score_orSumsDisjuncts() {
		assertThatQuery( index.query()
				.where( f -> f.or()
						.add( f.match().field( "title" ).matching( MATCHING_TERM ).boost( TITLE_BOOST ) )
						.add( f.match().field( "description" ).matching( MATCHING_TERM ) )
						.add( f.match().field( "summary" ).matching( MATCHING_TERM ) ) ) )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	void score_bestDisjunctOnly() {
		assertThatQuery( index.query()
				.where( f -> f.disjunctionMax()
						.add( f.match().field( "title" ).matching( MATCHING_TERM ).boost( TITLE_BOOST ) )
						.add( f.match().field( "description" ).matching( MATCHING_TERM ) )
						.add( f.match().field( "summary" ).matching( MATCHING_TERM ) ) ) )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	void tieBreaker_scoresLikeOr() {
		assertThatQuery( index.query()
				.where( f -> f.disjunctionMax()
						.add( f.match().field( "title" ).matching( MATCHING_TERM ).boost( TITLE_BOOST ) )
						.add( f.match().field( "description" ).matching( MATCHING_TERM ) )
						.add( f.match().field( "summary" ).matching( MATCHING_TERM ) )
						.tieBreaker( 1.0f ) ) )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	void nestedInsideBool() {
		assertThatQuery( index.query()
				.where( f -> f.bool()
						.must( f.disjunctionMax()
								.add( f.match().field( "description" ).matching( MATCHING_TERM ) )
								.add( f.match().field( "summary" ).matching( MATCHING_TERM ) ) )
						.mustNot( f.match().field( "title" ).matching( MATCHING_TERM ) ) ) )
				.hasTotalHitCount( 1 )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	// Every field holds exactly one token in both documents, and the matching term appears in each field in
	// exactly one of them: field length, per-field document count and term document frequency are therefore
	// equal, and only the boost and the way scores are combined can change the order.
	private static void initData() {
		BulkIndexer indexer = index.bulkIndexer();
		indexer.add( DOCUMENT_1, document -> {
			document.addValue( index.binding().title, MATCHING_TERM );
			document.addValue( index.binding().description, OTHER_TERM );
			document.addValue( index.binding().summary, OTHER_TERM );
		} );
		indexer.add( DOCUMENT_2, document -> {
			document.addValue( index.binding().title, OTHER_TERM );
			document.addValue( index.binding().description, MATCHING_TERM );
			document.addValue( index.binding().summary, MATCHING_TERM );
		} );
		indexer.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> title;
		final IndexFieldReference<String> description;
		final IndexFieldReference<String> summary;

		IndexBinding(IndexSchemaElement root) {
			title = root.field( "title",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) ).toReference();
			description = root.field( "description",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) ).toReference();
			summary = root.field( "summary",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) ).toReference();
		}
	}
}
