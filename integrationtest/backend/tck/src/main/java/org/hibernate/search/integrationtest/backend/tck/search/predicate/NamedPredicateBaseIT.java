/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.function.Function;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProvider;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProviderContext;
import org.hibernate.search.engine.search.query.dsl.SearchQueryFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class NamedPredicateBaseIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";
	private static final String EMPTY = "empty";

	private static final String TEXT_MATCHING_1 = "Localization in English is a must-have.";
	private static final String TEXT_MATCHING_2 = "Internationalization allows to adapt the application to multiple locales.";
	private static final String TEXT_MATCHING_3 = "A had to call the land lord.";
	private static final String TEXT_MATCHING_4 = "I had some interaction with that lad.";

	private static final String TERM_PATTERN_1 = "Localization";
	private static final String TERM_PATTERN_2 = "Internationalization";
	private static final String TERM_PATTERN_3 = "call";
	private static final String TERM_PATTERN_4 = "interaction";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final DataSet dataSet = new DataSet();

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		dataSet.contribute( indexer );
		indexer.join();
	}

	@Test
	public void searchNamedPredicateInRoot() {
		StubMappingScope scope = index.createScope();
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
			.where( f -> f.named( "match_predicate" )
			.param( "query", queryString ) );

		assertThatQuery( createQuery.apply( TERM_PATTERN_1 ) )
			.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_2 ) )
			.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_3 ) )
			.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_4 ) )
			.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );
	}

	@Test
	public void searchNamedPredicateInNested() {
		StubMappingScope scope = index.createScope();
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
			.where( f -> f.named( "nested.match_predicate" )
			.param( "query", queryString ) );

		assertThatQuery( createQuery.apply( TERM_PATTERN_1 ) )
			.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_2 ) )
			.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_3 ) )
			.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_PATTERN_4 ) )
			.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> field1;
		final SimpleFieldModel<String> field2;
		final IndexObjectFieldReference nested;

		IndexBinding(IndexSchemaElement root) {
			field1 = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
				c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
				.map( root, "tested_field" );

			IndexSchemaObjectField nest = root.objectField( "nested", ObjectStructure.NESTED ).multiValued();
			nested = nest.toReference();

			field2 = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
				c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
				.map( nest, "tested_field" );

			root.namedPredicate( "match_predicate", new TestNamedPredicateProvider() );

			nest.namedPredicate( "match_predicate", new TestNamedPredicateProvider() );
		}
	}

	private static final class DataSet extends AbstractPredicateDataSet {
		public DataSet() {
			super( null );
		}

		public void contribute(BulkIndexer indexer) {
			indexer
				.add( DOCUMENT_1, document -> {
					document.addValue( index.binding().field1.reference, TEXT_MATCHING_1 );
					IndexObjectFieldReference level1 = index.binding().nested;
					DocumentElement nested = document.addObject( level1 );
					nested.addValue( index.binding().field2.reference, TEXT_MATCHING_1 );

				} )
				.add( DOCUMENT_2, document -> {
					document.addValue( index.binding().field1.reference, TEXT_MATCHING_2 );
					IndexObjectFieldReference level1 = index.binding().nested;
					DocumentElement nested = document.addObject( level1 );
					nested.addValue( index.binding().field2.reference, TEXT_MATCHING_2 );
				} )
				.add( DOCUMENT_3, document -> {
					document.addValue( index.binding().field1.reference, TEXT_MATCHING_3 );
					IndexObjectFieldReference level1 = index.binding().nested;
					DocumentElement nested = document.addObject( level1 );
					nested.addValue( index.binding().field2.reference, TEXT_MATCHING_3 );
				} )
				.add( DOCUMENT_4, document -> {
					document.addValue( index.binding().field1.reference, TEXT_MATCHING_4 );
					IndexObjectFieldReference level1 = index.binding().nested;
					DocumentElement nested = document.addObject( level1 );
					nested.addValue( index.binding().field2.reference, TEXT_MATCHING_4 );
				} )
				.add( EMPTY, document -> {
				} );
		}
	}

	public static class TestNamedPredicateProvider implements NamedPredicateProvider {

		@Override
		public SearchPredicate create(NamedPredicateProviderContext context) {
			String absoluteFieldPath = context.absolutePath( "tested_field" );
			String queryString = (String) context.param( "query" );
			SearchPredicate predicate = context.predicate()
				.match().field( absoluteFieldPath )
				.matching( queryString )
				.toPredicate();
			return predicate;
		}
	}

}
