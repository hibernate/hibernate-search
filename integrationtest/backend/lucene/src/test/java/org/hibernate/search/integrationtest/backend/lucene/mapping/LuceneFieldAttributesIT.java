/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
import org.apache.lucene.analysis.payloads.TokenOffsetPayloadTokenFilterFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.assertj.core.api.Assertions;

public class LuceneFieldAttributesIT {

	private static final String BACKEND_NAME = "my-backend";
	private static final String INDEX_NAME = "my-index";
	private static final String ANALYZER_NAME = "my-analyzer";

	private static final String TEXT = "This is a text containing things. Red house with a blue carpet on the road...";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withBackendProperty( BACKEND_NAME, LuceneBackendSettings.ANALYSIS_CONFIGURER,
						(LuceneAnalysisConfigurer) ctx -> ctx
								.analyzer( ANALYZER_NAME ).custom()
								.tokenizer( NGramTokenizerFactory.class )
								.param( "minGramSize", "5" )
								.param( "maxGramSize", "6" )
								.tokenFilter( TokenOffsetPayloadTokenFilterFactory.class )
				)
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void verifyNorms() {
		Document document = loadDocument();

		// norms false => omit-norms true
		Assertions.assertThat( document.getField( "keyword" ).fieldType().omitNorms() ).isTrue();
		Assertions.assertThat( document.getField( "noNorms" ).fieldType().omitNorms() ).isTrue();

		// norms true => omit-norms false
		Assertions.assertThat( document.getField( "text" ).fieldType().omitNorms() ).isFalse();
		Assertions.assertThat( document.getField( "norms" ).fieldType().omitNorms() ).isFalse();
	}

	@Test
	public void verifyTermVector() {
		Document document = loadDocument();

		IndexableField field = document.getField( "text" );
		// default no term vector stored
		Assertions.assertThat( field.fieldType().storeTermVectors() ).isFalse();
		Assertions.assertThat( field.fieldType().storeTermVectorPositions() ).isFalse();
		Assertions.assertThat( field.fieldType().storeTermVectorOffsets() ).isFalse();
		Assertions.assertThat( field.fieldType().storeTermVectorPayloads() ).isFalse();

		field = document.getField( "termVector" );
		Assertions.assertThat( field.fieldType().storeTermVectors() ).isTrue();
		Assertions.assertThat( field.fieldType().storeTermVectorPositions() ).isFalse();
		Assertions.assertThat( field.fieldType().storeTermVectorOffsets() ).isFalse();
		Assertions.assertThat( field.fieldType().storeTermVectorPayloads() ).isFalse();

		field = document.getField( "moreOptions" );
		Assertions.assertThat( field.fieldType().storeTermVectors() ).isTrue();
		// TODO these are not true:
		// Assertions.assertThat( field.fieldType().storeTermVectorPositions() ).isTrue();
		// Assertions.assertThat( field.fieldType().storeTermVectorOffsets() ).isTrue();
		// Assertions.assertThat( field.fieldType().storeTermVectorPayloads() ).isTrue();
	}

	private Document loadDocument() {
		SearchQuery<Document> query = indexManager.createScope().query()
				.asProjection(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetch().getHits();

		Assertions.assertThat( result ).hasSize( 1 );
		return result.get( 0 );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "ID:1" ), document -> {
			document.addValue( indexMapping.string, "keyword" );
			document.addValue( indexMapping.text, TEXT );
			document.addValue( indexMapping.norms, TEXT );
			document.addValue( indexMapping.noNorms, TEXT );
			document.addValue( indexMapping.termVector, TEXT );
			document.addValue( indexMapping.moreOptions, "Search 6 groundwork - Add the missing common field type options compared to Search 5" );
		} );

		workPlan.execute().join();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> text;
		final IndexFieldReference<String> norms;
		final IndexFieldReference<String> noNorms;
		final IndexFieldReference<String> termVector;
		final IndexFieldReference<String> moreOptions;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "keyword", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			text = root.field( "text", f -> f.asString().analyzer( ANALYZER_NAME ).projectable( Projectable.YES ) ).toReference();

			norms = root.field( "norms", f -> {
				// extracting a variable to workaround an Eclipse compiler issue
				StringIndexFieldTypeOptionsStep<?> ctx = f.asString()
						.analyzer( ANALYZER_NAME ).projectable( Projectable.YES );
				return ctx.norms( Norms.YES ); }
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
				return ctx.termVector( TermVector.YES ); }
			).toReference();

			moreOptions = root.field( "moreOptions", f -> {
				// extracting a variable to workaround an Eclipse compiler issue
				StringIndexFieldTypeOptionsStep<?> ctx = f.asString()
						.analyzer( ANALYZER_NAME ).projectable( Projectable.YES );
				return ctx.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS ); }
			).toReference();
		}
	}
}
