/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.analysis.AnalyzerDescriptor;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SingleFieldDocumentBuilder;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test indexing and searching with built-in analyzer definitions overridden by the user
 * See {@link AnalyzerNames}.
 * <p>
 * Analyzers are expected to be overridden in such a way that they replace any text with their own name,
 * so that we can easily identify whether the override works or not.
 */
class AnalysisBuiltinOverrideIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start( TckBackendHelper::createAnalysisBuiltinOverridesBackendSetupStrategy ).withIndex( index ).setup();
	}

	@Test
	void analyzer_default() {
		verifyOverride( index.binding().defaultAnalyzer );
	}

	@Test
	void analyzer_standard() {
		verifyOverride( index.binding().standardAnalyzer );
	}

	@Test
	void analyzer_simple() {
		verifyOverride( index.binding().simpleAnalyzer );
	}

	@Test
	void analyzer_whitespace() {
		verifyOverride( index.binding().whitespaceAnalyzer );
	}

	@Test
	void analyzer_stop() {
		verifyOverride( index.binding().stopAnalyzer );
	}

	@Test
	void analyzer_keyword() {
		verifyOverride( index.binding().keywordAnalyzer );
	}

	@Test
	void indexAnalyzerDescriptor() {
		Collection<? extends AnalyzerDescriptor> analyzerDescriptors = index.toApi().descriptor().analyzers();
		assertThat( analyzerDescriptors )
				.isNotEmpty()
				.extracting( AnalyzerDescriptor::name )
				.containsExactlyInAnyOrder(
						AnalyzerNames.DEFAULT,
						AnalyzerNames.STOP,
						AnalyzerNames.KEYWORD,
						AnalyzerNames.WHITESPACE,
						AnalyzerNames.SIMPLE,
						AnalyzerNames.STANDARD
				);

		for ( AnalyzerDescriptor descriptor : analyzerDescriptors ) {
			assertThat( index.toApi().descriptor().analyzer( descriptor.name() ) )
					.isPresent()
					.get()
					.isEqualTo( descriptor );
		}
	}

	private void verifyOverride(SimpleFieldModel<String> field) {
		initData( field, b -> {
			b.emptyDocument( "empty" );
			b.document( "nonempty", "somewords" );
		} );

		// If the analyzer was successfully overridden, this won't match:
		assertMatchQuery( field, "somewords" )
				.hasDocRefHitsAnyOrder( index.typeName(), "nonempty" );

		// ... but this will:
		assertMatchQuery( field, AnalyzerNames.DEFAULT )
				.hasDocRefHitsAnyOrder( index.typeName(), "nonempty" );
	}

	private SearchResultAssert<DocumentReference> assertMatchQuery(SimpleFieldModel<String> fieldModel, String valueToMatch) {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( fieldModel.relativeFieldName ).matching( valueToMatch ) )
				.toQuery();

		return assertThatQuery( query );
	}

	private void initData(SimpleFieldModel<String> fieldModel, Consumer<SingleFieldDocumentBuilder<String>> valueContributor) {
		index.bulkIndexer()
				.add( fieldModel.reference, valueContributor )
				.join();
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> defaultAnalyzer;
		final SimpleFieldModel<String> standardAnalyzer;
		final SimpleFieldModel<String> simpleAnalyzer;
		final SimpleFieldModel<String> whitespaceAnalyzer;
		final SimpleFieldModel<String> stopAnalyzer;
		final SimpleFieldModel<String> keywordAnalyzer;

		IndexBinding(IndexSchemaElement root) {
			this.defaultAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.DEFAULT ) )
					.map( root, "default" );
			this.standardAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.STANDARD ) )
					.map( root, "standard" );
			this.simpleAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.SIMPLE ) )
					.map( root, "simple" );
			this.whitespaceAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.WHITESPACE ) )
					.map( root, "whitespace" );
			this.stopAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.STOP ) )
					.map( root, "stop" );
			this.keywordAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.KEYWORD ) )
					.map( root, "keyword" );
		}
	}
}
