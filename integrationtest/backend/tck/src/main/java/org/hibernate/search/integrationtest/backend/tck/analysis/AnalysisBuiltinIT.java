/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SingleFieldDocumentBuilder;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test indexing and searching with built-in analyzer definitions.
 * See {@link AnalyzerNames}.
 */
@RunWith(Parameterized.class)
public class AnalysisBuiltinIT {

	@Parameterized.Parameters
	public static List<SearchSetupHelper> params() {
		return Arrays.asList(
				// Test with no analysis configurer whatsoever
				new SearchSetupHelper( TckBackendHelper::createAnalysisNotConfiguredBackendSetupStrategy ),
				// Test with an analysis configurer that does not override the defaults but defines other analyzers
				new SearchSetupHelper( TckBackendHelper::createAnalysisCustomBackendSetupStrategy )
		);
	}

	@Rule
	public final SearchSetupHelper setupHelper;

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	public AnalysisBuiltinIT(SearchSetupHelper setupHelper) {
		this.setupHelper = setupHelper;
	}

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void analyzer_default() {
		SimpleFieldModel<String> field = index.binding().defaultAnalyzer;

		initData( field, b -> {
			b.emptyDocument( "empty" );
			b.document( "1", "twowords" );
			b.document( "2", "two.words" );
			b.document( "3", "two-words" );
			b.document( "4", "two words" );
			b.document( "5", "TWO WORDS" );
			b.document( "6", "two wôrds" );
			b.document( "7", "a stopword the stopword" );
		} );

		// Tokenize on space, hyphen
		assertMatchQuery( field, "words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );

		// Case-insensitive
		assertMatchQuery( field, "WORDS" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );
		assertMatchQuery( field, "Words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );

		// No stopword removal
		assertMatchQuery( field, "a" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
		assertMatchQuery( field, "the" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
	}

	private SearchResultAssert<DocumentReference> assertMatchQuery(SimpleFieldModel<String> fieldModel, String valueToMatch) {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( fieldModel.relativeFieldName ).matching( valueToMatch ) )
				.toQuery();

		return assertThat( query );
	}

	private void initData(SimpleFieldModel<String> fieldModel, Consumer<SingleFieldDocumentBuilder<String>> valueContributor) {
		index.bulkIndexer()
				.add( fieldModel.reference, valueContributor )
				.join();
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> defaultAnalyzer;

		IndexBinding(IndexSchemaElement root) {
			this.defaultAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.DEFAULT ) )
					.map( root, "default" );
		}
	}

}
