/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

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

/**
 * Test indexing and searching with built-in analyzer definitions overridden by the user
 * See {@link AnalyzerNames}.
 * <p>
 * Analyzers are expected to be overridden in such a way that they replace any text with their own name,
 * so that we can easily identify whether the override works or not.
 */
public class AnalysisBuiltinOverrideIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper( TckBackendHelper::createAnalysisBuiltinOverridesBackendSetupStrategy );

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void analyzer_default() {
		SimpleFieldModel<String> field = index.binding().defaultAnalyzer;

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
