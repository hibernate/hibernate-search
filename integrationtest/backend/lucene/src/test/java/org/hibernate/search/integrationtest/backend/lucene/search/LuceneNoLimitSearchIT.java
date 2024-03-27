/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-3947")

class LuceneNoLimitSearchIT {

	public static final int INDEX_SIZE = 100_000;

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
		initData();
	}

	@Test
	void fetchAll() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "field" ).matching( "739" ) )
				.toQuery();

		SearchResult<DocumentReference> documentReferences = query.fetchAll();

		assertThatResult( documentReferences )
				.hasDocRefHitsAnyOrder( index.typeName(), "739" )
				.hasTotalHitCount( 1L );
	}

	@Test
	void fetchAll_lessThan100Matches() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.range().field( "field" ).between( "73979", "73997" ) )
				.toQuery();

		SearchResult<DocumentReference> documentReferences = query.fetchAll();

		assertThatResult( documentReferences ).hasTotalHitCount( 21L );
	}

	@Test
	void fetchAll_between100And10000Matches() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.range().field( "field" ).between( "73937", "79397" ) )
				.toQuery();

		SearchResult<DocumentReference> documentReferences = query.fetchAll();

		assertThatResult( documentReferences ).hasTotalHitCount( 6067L );
	}

	@Test
	void fetchAll_moreThan10000Matches() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.range().field( "field" ).between( "37973", "73937" ) )
				.toQuery();

		SearchResult<DocumentReference> documentReferences = query.fetchAll();

		assertThatResult( documentReferences ).hasTotalHitCount( 39961L );
	}

	@Test
	void fetchAll_totalHitCountThreshold() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "field" ).matching( "739" ) )
				.totalHitCountThreshold( 5 )
				.toQuery();

		SearchResult<DocumentReference> documentReferences = query.fetchAll();

		assertThatResult( documentReferences )
				.hasDocRefHitsAnyOrder( index.typeName(), "739" )
				.hasTotalHitCount( 1L );
	}

	@Test
	void fetchAllHits() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "field" ).matching( "739" ) )
				.toQuery();

		List<DocumentReference> documentReferences = query.fetchAllHits();

		assertThatHits( documentReferences ).hasDocRefHitsAnyOrder( index.typeName(), "739" );
	}

	private static void initData() {
		index.bulkIndexer()
				.add( INDEX_SIZE, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( index.binding().field, String.valueOf( i ) )
				) )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> field;

		IndexBinding(IndexSchemaElement root) {
			field = root.field( "field", c -> c.asString() ).toReference();
		}
	}
}
