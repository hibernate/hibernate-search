/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SingleFieldDocumentBuilder;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-1302")
public class StopwordAnalyzerIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void analyzer_whitespace() {
		SimpleFieldModel<String> field = index.binding().whitespaceAnalyzer;
		initData( field );

		// No stopword removal
		assertMatchQuery( field, "the" ).hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );
		assertMatchQuery( field, "life" ).hasDocRefHitsAnyOrder( index.typeName(), "2", "3" );
	}

	@Test
	public void analyzer_stop() {
		SimpleFieldModel<String> field = index.binding().stopAnalyzer;
		initData( field );

		// Stopword removal
		assertMatchQuery( field, "the" ).hasNoHits();
		assertMatchQuery( field, "life" ).hasDocRefHitsAnyOrder( index.typeName(), "2", "3" );
	}

	private SearchResultAssert<DocumentReference> assertMatchQuery(SimpleFieldModel<String> fieldModel,
			String valueToMatch) {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( fieldModel.relativeFieldName ).matching( valueToMatch ) )
				.toQuery();

		return assertThatQuery( query );
	}

	private void initData(SimpleFieldModel<String> fieldModel) {
		index.bulkIndexer()
				.add( fieldModel.reference, StopwordAnalyzerIT::buildDocuments )
				.join();
	}

	private static void buildDocuments(SingleFieldDocumentBuilder<String> builder) {
		builder.emptyDocument( "empty" );
		builder.document( "1", "the a" ); // only stopwords (see HSEARCH-1302)
		builder.document( "2", "the life" ); // stopword + non-stopword
		builder.document( "3", "true life" ); // only non-stopwords
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> whitespaceAnalyzer;
		final SimpleFieldModel<String> stopAnalyzer;

		IndexBinding(IndexSchemaElement root) {
			this.whitespaceAnalyzer = SimpleFieldModel.mapperWithOverride(
					KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.WHITESPACE )
			)
					.map( root, "whitespace" );
			this.stopAnalyzer = SimpleFieldModel.mapperWithOverride(
					KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.STOP )
			)
					.map( root, "stop" );
		}
	}
}
