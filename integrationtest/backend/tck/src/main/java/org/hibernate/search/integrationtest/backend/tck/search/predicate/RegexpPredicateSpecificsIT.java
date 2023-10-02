/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collections;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.dsl.RegexpQueryFlag;
import org.hibernate.search.engine.search.query.dsl.SearchQueryFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RegexpPredicateSpecificsIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";
	private static final String DOCUMENT_5 = "document5";
	private static final String EMPTY = "empty";

	// taken from the current project documentation:
	private static final String TEXT_1 =
			"Hibernate Search will transparently index every entity persisted, updated or removed through Hibernate ORM";
	private static final String TEXT_2 = "The above paragraphs gave you an overview of Hibernate Search";
	private static final String TEXT_3 =
			"Applications targeted by Hibernate search generally use an entity-based model to represent data.";
	private static final String TEXT_4 =
			"     Hibernate        Search   will transparently index every entity persisted, updated or removed through Hibernate ORM";
	private static final String TEXT_5 = "7.39";

	private static final String TEXT_INTERVAL = "foo<1-100>";
	private static final String TEXT_INTERVAL_MATCHING = "foo99";
	private static final String TEXT_INTERVAL_NOT_MATCHING = "foo101";

	private static final String TEXT_INTERSECTION = "aaa.+&.+bbb";
	private static final String TEXT_INTERSECTION_MATCHING = "aaabbb";
	private static final String TEXT_INTERSECTION_NOT_MATCHING = "aabbb";

	private static final String TEXT_ANYSTRING = "@abc";
	private static final String TEXT_ANYSTRING_MATCHING = "abcabc";
	private static final String TEXT_ANYSTRING_NOT_MATCHING = "foo99";

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final DataSet dataSet = new DataSet();

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		dataSet.contribute( indexer );
		indexer.join();
	}

	@Test
	void analyzedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( "Hibernate.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2,
				DOCUMENT_3, DOCUMENT_4 );
		assertThatQuery( createQuery.apply( "Hibernate Search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "hibernate search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( "search.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2,
				DOCUMENT_3, DOCUMENT_4 );
		assertThatQuery( createQuery.apply( ".*search" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2,
				DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	void normalizedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().normalizedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( queryString ) );

		if ( TckConfiguration.get().getBackendFeatures().regexpExpressionIsNormalized() ) {
			assertThatQuery( createQuery.apply( "Hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
			assertThatQuery( createQuery.apply( "hibernate search.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		}
		else {
			assertThatQuery( createQuery.apply( "Hibernate.*" ) ).hasNoHits();
			assertThatQuery( createQuery.apply( "Hibernate Search.*" ) ).hasNoHits();
		}

		assertThatQuery( createQuery.apply( "hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( createQuery.apply( "hibernate search.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( createQuery.apply( "search.*" ) ).hasNoHits();
		assertThatQuery( createQuery.apply( ".*search" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	void nonAnalyzedField() {
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
	void moreCases() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;
		Function<String, SearchQueryFinalStep<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( queryString ) );

		assertThatQuery( createQuery.apply( "(\\ )+Hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );
		assertThatQuery( createQuery.apply( "(\\ )*Hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1,
				DOCUMENT_4 );
		assertThatQuery( createQuery.apply( "(\\ )?Hibernate.*" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( createQuery.apply( "[739]+\\.[739]+" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_5 );
		assertThatQuery( createQuery.apply( "[739]+(\\.)?[739]+" ) ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_5 );
		assertThatQuery( createQuery.apply( "[739]+(\\.)?[79]+" ) ).hasNoHits();
	}

	@Test
	void emptyString() {
		String absoluteFieldPath = index.binding().analyzedField.relativeFieldName;

		assertThatQuery( index.query()
				.where( f -> f.regexp().field( absoluteFieldPath ).matching( "" ) ) )
				.hasNoHits();
	}

	@Test
	void flag_interval() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().intervalField.relativeFieldName;

		// test the default
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERVAL ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		// no flag at all (same as default)
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERVAL )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		// alone
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERVAL )
						.flags( RegexpQueryFlag.INTERVAL )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );

		// more flags
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERVAL )
						.flags( RegexpQueryFlag.INTERVAL, RegexpQueryFlag.ANY_STRING )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );

		// other flags only
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERVAL )
						.flags( RegexpQueryFlag.INTERSECTION, RegexpQueryFlag.ANY_STRING )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void flag_intersection() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().intersectionField.relativeFieldName;

		// test the default
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERSECTION ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		// no flag at all (same as default)
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERSECTION )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		// alone
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERSECTION )
						.flags( RegexpQueryFlag.INTERSECTION )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		// more flags
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERSECTION )
						.flags( RegexpQueryFlag.INTERVAL, RegexpQueryFlag.INTERSECTION )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		// other flags only
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_INTERSECTION )
						.flags( RegexpQueryFlag.INTERVAL, RegexpQueryFlag.ANY_STRING )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void flag_anyString() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().anyStringField.relativeFieldName;

		// test the default
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_ANYSTRING ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		// no flag at all (same as default)
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_ANYSTRING )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		// alone
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_ANYSTRING )
						.flags( RegexpQueryFlag.ANY_STRING )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		// more flags
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_ANYSTRING )
						.flags( RegexpQueryFlag.ANY_STRING, RegexpQueryFlag.INTERVAL, RegexpQueryFlag.INTERSECTION )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		// other flags only
		assertThatQuery( scope.query()
				.where( f -> f.regexp().field( absoluteFieldPath )
						.matching( TEXT_ANYSTRING )
						.flags( RegexpQueryFlag.INTERVAL, RegexpQueryFlag.INTERSECTION )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> analyzedField;
		final SimpleFieldModel<String> normalizedField;
		final SimpleFieldModel<String> nonAnalyzedField;

		// fields to test optional flags:
		final SimpleFieldModel<String> intervalField;
		final SimpleFieldModel<String> intersectionField;
		final SimpleFieldModel<String> anyStringField;

		IndexBinding(IndexSchemaElement root) {
			analyzedField = SimpleFieldModel.mapperWithOverride( AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
					.map( root, "analyzed" );
			normalizedField = SimpleFieldModel.mapperWithOverride( NormalizedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) )
					.map( root, "normalized" );
			nonAnalyzedField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "nonAnalyzed" );

			// fields to test optional flags:
			intervalField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "interval" );
			intersectionField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "intersection" );
			anyStringField = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "anyString" );
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
				document.addValue( index.binding().intervalField.reference, TEXT_INTERVAL );
				document.addValue( index.binding().intersectionField.reference, TEXT_INTERSECTION );
				document.addValue( index.binding().anyStringField.reference, TEXT_ANYSTRING );

			} )
					.add( DOCUMENT_2, document -> {
						document.addValue( index.binding().analyzedField.reference, TEXT_2 );
						document.addValue( index.binding().normalizedField.reference, TEXT_2 );
						document.addValue( index.binding().nonAnalyzedField.reference, TEXT_2 );
						document.addValue( index.binding().intervalField.reference, TEXT_INTERVAL_MATCHING );
						document.addValue( index.binding().intersectionField.reference, TEXT_INTERSECTION_MATCHING );
						document.addValue( index.binding().anyStringField.reference, TEXT_ANYSTRING_MATCHING );
					} )
					.add( DOCUMENT_3, document -> {
						document.addValue( index.binding().analyzedField.reference, TEXT_3 );
						document.addValue( index.binding().normalizedField.reference, TEXT_3 );
						document.addValue( index.binding().nonAnalyzedField.reference, TEXT_3 );
						document.addValue( index.binding().intervalField.reference, TEXT_INTERVAL_NOT_MATCHING );
						document.addValue(
								index.binding().intersectionField.reference, TEXT_INTERSECTION_NOT_MATCHING );
						document.addValue( index.binding().anyStringField.reference, TEXT_ANYSTRING_NOT_MATCHING );
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
					.add( EMPTY, document -> {} );
		}
	}
}
