/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;

import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.document.Document;

class LuceneFieldTypesIT {

	private static final String TEXT_1 = "This is a text containing things. Red house with a blue carpet on the road...";
	private static final String TEXT_2 = "This is a text containing other things. Such as move the line on the right margin...";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start()
				.withIndex( index )
				.setup();

		initData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1640")
	void verifyProjectionsOnDifferentTypes() {
		SearchQuery<Document> query = index.createScope().query()
				.select(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.where( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetchAll().hits();
		assertThat( result )
				.hasSize( 2 )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "keyword" )
								.hasField( "text", TEXT_1 )
								.hasField( "integer", 739 )
								.hasField( "longNumber", 739L )
								.hasField( "bool", 1 )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "anotherKeyword" )
								.hasField( "text", TEXT_2 )
								.hasField( "integer", 123 )
								.hasField( "longNumber", 123L )
								.hasField( "bool", 0 )
								.andOnlyInternalFields()
				) );
	}

	private void initData() {
		index.bulkIndexer()
				.add( "ID:1", document -> {
					document.addValue( index.binding().string, "keyword" );
					document.addValue( index.binding().text, TEXT_1 );
					document.addValue( index.binding().integer, 739 );
					document.addValue( index.binding().longNumber, 739L );
					document.addValue( index.binding().bool, true );
				} )
				.add( "ID:2", document -> {
					document.addValue( index.binding().string, "anotherKeyword" );
					document.addValue( index.binding().text, TEXT_2 );
					document.addValue( index.binding().integer, 123 );
					document.addValue( index.binding().longNumber, 123L );
					document.addValue( index.binding().bool, false );
				} )
				.join();
	}

	private static class IndexBinding {

		final IndexFieldReference<String> string;
		final IndexFieldReference<String> text;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<Long> longNumber;
		final IndexFieldReference<Boolean> bool;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			text = root.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.projectable( Projectable.YES ) ).toReference();
			integer = root.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
			longNumber = root.field( "longNumber", f -> f.asLong().projectable( Projectable.YES ) ).toReference();

			// the external form is the Boolean,
			// BUT we treat it as **Integer** for comparison operation (range, sort)
			// and we store it as **Integer** as well.
			bool = root.field( "bool", f -> f.asBoolean().projectable( Projectable.YES ) ).toReference();
		}
	}
}
