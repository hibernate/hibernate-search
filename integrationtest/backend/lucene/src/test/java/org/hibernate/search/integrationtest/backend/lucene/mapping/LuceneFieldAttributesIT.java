/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
import org.apache.lucene.analysis.payloads.TokenOffsetPayloadTokenFilterFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

class LuceneFieldAttributesIT {

	private static final String ANALYZER_NAME = "my-analyzer";

	private static final String TEXT = "This is a text containing things. Red house with a blue carpet on the road...";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start()
				.withBackendProperty( LuceneBackendSettings.ANALYSIS_CONFIGURER,
						(LuceneAnalysisConfigurer) ctx -> ctx
								.analyzer( ANALYZER_NAME ).custom()
								.tokenizer( NGramTokenizerFactory.class )
								.param( "minGramSize", "5" )
								.param( "maxGramSize", "6" )
								.tokenFilter( TokenOffsetPayloadTokenFilterFactory.class )
				)
				.withIndex( index )
				.setup();

		initData();
	}

	@Test
	void verifyNorms() {
		Document document = loadDocument();

		// norms false => omit-norms true
		assertThat( document.getField( "keyword" ).fieldType().omitNorms() ).isTrue();
		assertThat( document.getField( "noNorms" ).fieldType().omitNorms() ).isTrue();

		// norms true => omit-norms false
		assertThat( document.getField( "text" ).fieldType().omitNorms() ).isFalse();
		assertThat( document.getField( "norms" ).fieldType().omitNorms() ).isFalse();
	}

	@Test
	void verifyTermVector() {
		Document document = loadDocument();

		IndexableField field = document.getField( "text" );
		// default no term vector stored
		assertThat( field.fieldType().storeTermVectors() ).isFalse();
		assertThat( field.fieldType().storeTermVectorPositions() ).isFalse();
		assertThat( field.fieldType().storeTermVectorOffsets() ).isFalse();
		assertThat( field.fieldType().storeTermVectorPayloads() ).isFalse();

		field = document.getField( "termVector" );
		assertThat( field.fieldType().storeTermVectors() ).isTrue();
		assertThat( field.fieldType().storeTermVectorPositions() ).isFalse();
		assertThat( field.fieldType().storeTermVectorOffsets() ).isFalse();
		assertThat( field.fieldType().storeTermVectorPayloads() ).isFalse();

		field = document.getField( "moreOptions" );
		assertThat( field.fieldType().storeTermVectors() ).isTrue();
		// TODO these are not true:
		// assertThat( field.fieldType().storeTermVectorPositions() ).isTrue();
		// assertThat( field.fieldType().storeTermVectorOffsets() ).isTrue();
		// assertThat( field.fieldType().storeTermVectorPayloads() ).isTrue();
	}

	private Document loadDocument() {
		SearchQuery<Document> query = index.createScope().query()
				.select(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.where( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetchAll().hits();

		assertThat( result ).hasSize( 1 );
		return result.get( 0 );
	}

	private void initData() {
		index.bulkIndexer()
				.add( "ID:1", document -> {
					document.addValue( index.binding().string, "keyword" );
					document.addValue( index.binding().text, TEXT );
					document.addValue( index.binding().norms, TEXT );
					document.addValue( index.binding().noNorms, TEXT );
					document.addValue( index.binding().termVector, TEXT );
					document.addValue( index.binding().moreOptions,
							"Search 6 groundwork - Add the missing common field type options compared to Search 5" );
				} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> text;
		final IndexFieldReference<String> norms;
		final IndexFieldReference<String> noNorms;
		final IndexFieldReference<String> termVector;
		final IndexFieldReference<String> moreOptions;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "keyword", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			text = root.field( "text", f -> f.asString().analyzer( ANALYZER_NAME ).projectable( Projectable.YES ) )
					.toReference();

			norms = root.field( "norms", f -> {
				// extracting a variable to workaround an Eclipse compiler issue
				StringIndexFieldTypeOptionsStep<?> ctx = f.asString()
						.analyzer( ANALYZER_NAME ).projectable( Projectable.YES );
				return ctx.norms( Norms.YES );
			}
			).toReference();

			noNorms = root.field( "noNorms", f -> {
				// extracting a variable to workaround an Eclipse compiler issue
				StringIndexFieldTypeOptionsStep<?> ctx = f.asString()
						.analyzer( ANALYZER_NAME ).projectable( Projectable.YES );
				return ctx.norms( Norms.NO );
			} ).toReference();

			termVector = root.field( "termVector", f -> {
				// extracting a variable to workaround an Eclipse compiler issue
				StringIndexFieldTypeOptionsStep<?> ctx = f.asString()
						.analyzer( ANALYZER_NAME ).projectable( Projectable.YES );
				return ctx.termVector( TermVector.YES );
			}
			).toReference();

			moreOptions = root.field( "moreOptions", f -> {
				// extracting a variable to workaround an Eclipse compiler issue
				StringIndexFieldTypeOptionsStep<?> ctx = f.asString()
						.analyzer( ANALYZER_NAME ).projectable( Projectable.YES );
				return ctx.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS );
			}
			).toReference();
		}
	}
}
