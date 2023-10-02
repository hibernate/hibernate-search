/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Arrays;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test the DSL when the id of the entity is stored as a different type.
 * <p>
 * In this test the id of the entity is an integer but it's stored in the index, using a converter,
 * as a string with the prefix `document`. In the DSL the user will still use the integer type when
 * looking for entities matching an id.
 */

class MatchIdPredicateConverterIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final StubMappedIndex index = StubMappedIndex.ofAdvancedNonRetrievable( ctx -> ctx
			.idDslConverter( Integer.class, (value, context) -> "document" + value ) );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	void match_id() {
		assertThatQuery( index.query()
				.where( f -> f.id().matching( 1 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void match_multiple_ids() {
		assertThatQuery( index.query()
				.where( f -> f.id()
						.matching( 1 )
						.matching( 3 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	void match_any_and_match_single_id() {
		assertThatQuery( index.query()
				.where( f -> f.id()
						.matching( 2 )
						.matchingAny( Arrays.asList( 1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	void match_any_single_id() {
		assertThatQuery( index.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( 1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void match_any_ids() {
		assertThatQuery( index.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( 1, 3 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	private static void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {} )
				.add( DOCUMENT_2, document -> {} )
				.add( DOCUMENT_3, document -> {} )
				.join();
	}
}
