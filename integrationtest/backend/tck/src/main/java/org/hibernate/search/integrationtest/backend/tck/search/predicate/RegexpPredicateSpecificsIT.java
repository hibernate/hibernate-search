/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.dsl.SearchQueryFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class RegexpPredicateSpecificsIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";
	private static final String DOCUMENT_5 = "document5";
	private static final String EMPTY = "empty";

	// taken from the current project documentation:
	private static final String TEXT_1 = "Hibernate Search will transparently index every entity persisted, updated or removed through Hibernate ORM";
	private static final String TEXT_2 = "The above paragraphs gave you an overview of Hibernate Search";
	private static final String TEXT_3 = "Applications targeted by Hibernate search generally use an entity-based model to represent data.";
	private static final String TEXT_4 = "     Hibernate        Search   will transparently index every entity persisted, updated or removed through Hibernate ORM";
	private static final String TEXT_5 = "7.39";

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
	public void analyzedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( "Hibernate.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
		assertThatQuery( createQuery.apply( "Hibernate Search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "hibernate search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "search.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
		assertThatQuery( createQuery.apply( ".*search" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void normalizedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().normalizedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( "Hibernate.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( createQuery.apply( "Hibernate Search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "hibernate search.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( createQuery.apply( "search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( ".*search" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	public void nonAnalyzedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( "Hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( createQuery.apply( "hibernate.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "Hibernate Search.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( createQuery.apply( "hibernate search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( ".*search" ) ).hasNoHits();
	}

	@Test
	public void moreCases() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( "(\\ )+Hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );
		assertThatQuery( createQuery.apply( "(\\ )*Hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_4 );
		assertThatQuery( createQuery.apply( "(\\ )?Hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( "[739]+\\.[739]+" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_5 );
		assertThatQuery( createQuery.apply( "[739]+(\\.)?[739]+" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_5 );
		assertThatQuery( createQuery.apply( "[739]+(\\.)?[79]+" ) ).hasNoHits();
	}

	@Test
	public void emptyString() {
		String absoluteFieldPath = index.binding().analyzedField.relativeFieldName;

		assertThatQuery( index.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( "" ) ) )
				.hasNoHits();
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> analyzedField;
		final SimpleFieldModel<String> normalizedField;
		final SimpleFieldModel<String> nonAnalyzedField;

		IndexBinding(IndexSchemaElement root) {
			analyzedField = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
					.map( root, "analyzed" );
			normalizedField = SimpleFieldModel.mapperWithOverride( NormalizedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) )
					.map( root, "normalized" );
			nonAnalyzedField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "nonAnalyzed" );
		}
	}

	private static final class DataSet extends AbstractPredicateDataSet {
		protected DataSet() {
			super( null );
		}

		public void contribute(BulkIndexer indexer) {
			indexer.add( DOCUMENT_1, document -> {
						document.addValue( index.binding().analyzedField.reference, TEXT_1 );
						document.addValue( index.binding().normalizedField.reference, TEXT_1 );
						document.addValue( index.binding().nonAnalyzedField.reference, TEXT_1 );
					} )
					.add( DOCUMENT_2, document -> {
						document.addValue( index.binding().analyzedField.reference, TEXT_2 );
						document.addValue( index.binding().normalizedField.reference, TEXT_2 );
						document.addValue( index.binding().nonAnalyzedField.reference, TEXT_2 );
					} )
					.add( DOCUMENT_3, document -> {
						document.addValue( index.binding().analyzedField.reference, TEXT_3 );
						document.addValue( index.binding().normalizedField.reference, TEXT_3 );
						document.addValue( index.binding().nonAnalyzedField.reference, TEXT_3 );
					} )
					.add( DOCUMENT_4, document -> {
						document.addValue( index.binding().analyzedField.reference, TEXT_4 );
						document.addValue( index.binding().normalizedField.reference, TEXT_4 );
						document.addValue( index.binding().nonAnalyzedField.reference, TEXT_4 );
					} )
					.add( DOCUMENT_5, document -> {
						document.addValue( index.binding().analyzedField.reference, TEXT_5 );
						document.addValue( index.binding().normalizedField.reference, TEXT_5 );
						document.addValue( index.binding().nonAnalyzedField.reference, TEXT_5 );
					} )
					.add( EMPTY, document -> { } );
		}
	}
}
