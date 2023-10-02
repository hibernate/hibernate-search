/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.MonthDay;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.AbstractObjectBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests basic behavior of sorts by field value common to all supported types.
 */

class FieldSortSpecificsIT<F> {

	private static final Set<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new LinkedHashSet<>();
	private static final List<DataSet<?>> dataSets = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( StandardFieldTypeDescriptor<?> fieldType : StandardFieldTypeDescriptor.getAllStandard() ) {
			if ( fieldType.isFieldSortSupported() ) {
				supportedFieldTypes.add( fieldType );
				for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
					// We need two separate datasets when the sort mode is not defined,
					// because then the sort mode will be inferred automatically to
					// MIN for desc order, or MAX for asc order.
					DataSet<?> dataSetForAsc = new DataSet<>( fieldStructure, fieldType, null, SortMode.MIN );
					dataSets.add( dataSetForAsc );
					DataSet<?> dataSetForDesc = new DataSet<>( fieldStructure, fieldType, null, SortMode.MAX );
					dataSets.add( dataSetForDesc );
					parameters.add( Arguments.of( fieldStructure, fieldType, null, dataSetForAsc, dataSetForDesc ) );
					for ( SortMode sortMode : SortMode.values() ) {
						// When the sort mode is defined, we only need one dataset.
						dataSetForAsc = new DataSet<>( fieldStructure, fieldType, sortMode, sortMode );
						dataSets.add( dataSetForAsc );
						dataSetForDesc = dataSetForAsc;
						parameters.add( Arguments.of( fieldStructure, fieldType, sortMode, dataSetForAsc, dataSetForDesc ) );
					}
				}
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	private static final int BEFORE_DOCUMENT_1_ORDINAL = 0;
	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int BETWEEN_DOCUMENT_1_AND_2_ORDINAL = 2;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int BETWEEN_DOCUMENT_2_AND_3_ORDINAL = 4;
	private static final int DOCUMENT_3_ORDINAL = 5;
	private static final int AFTER_DOCUMENT_3_ORDINAL = 6;

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final Function<IndexSchemaElement, SingleFieldIndexBinding> bindingFactory =
			root -> SingleFieldIndexBinding.create( root, supportedFieldTypes, c -> c.sortable( Sortable.YES ) );

	private static final SimpleMappedIndex<SingleFieldIndexBinding> index = SimpleMappedIndex.of( bindingFactory );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.contribute( indexer );
		}
		indexer.join();
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = { "HSEARCH-3798", "HSEARCH-2252", "HSEARCH-2254", "HSEARCH-3103" })
	void simple(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTestParametersWork( fieldStructure, fieldType, sortMode );

		DataSet<F> dataSet;
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath( fieldStructure, fieldType );

		// Default order
		dataSet = dataSetForAsc;
		query = matchNonEmptyQuery( dataSet, b -> b.field( fieldPath ), sortMode, fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );

		// Explicit order
		dataSet = dataSetForAsc;
		query = matchNonEmptyQuery( dataSet, b -> b.field( fieldPath ).asc(), sortMode, fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyQuery( dataSet, b -> b.field( fieldPath ).desc(), sortMode, fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	void medianWithNestedField(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTrue(
				isMedianWithNestedField( sortMode, fieldStructure )
						&& !isSumOrAvgOrMedianWithStringField( sortMode, fieldType ),
				"This test is only relevant when using SortMode.MEDIAN in nested fields"
		);

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatThrownBy( () -> matchNonEmptyQuery( dataSetForAsc, b -> b.field( fieldPath ), sortMode,
				fieldStructure
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: MEDIAN",
						"This sort mode is not supported for fields in nested documents",
						fieldPath
				);
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	void sumOrAvgOrMedianWithStringField(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTrue(
				isSumOrAvgOrMedianWithStringField( sortMode, fieldType ),
				"This test is only relevant when using SortMode.SUM/AVG/MEDIAN on String fields"
		);

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatThrownBy( () -> matchNonEmptyQuery( dataSetForAsc, b -> b.field( fieldPath ), sortMode,
				fieldStructure
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: " + sortMode.name() + ". This sort mode is not supported for String fields",
						"Only MIN and MAX are supported",
						fieldPath
				);
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	void sumWithTemporalField(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTrue(
				isSumWithTemporalField( sortMode, fieldType ),
				"This test is only relevant when using SortMode.SUM on Temporal fields"
		);

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatThrownBy( () -> matchNonEmptyQuery( dataSetForAsc, b -> b.field( fieldPath ), sortMode,
				fieldStructure
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: SUM. This sort mode is not supported for temporal fields",
						"Only MIN, MAX, AVG and MEDIAN are supported",
						fieldPath
				);
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3886")
	void missingValue_default(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTestParametersWork( fieldStructure, fieldType, sortMode );

		DataSet<F> dataSet;
		SearchQuery<DocumentReference> query;

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		// Default for missing values is last, regardless of the order

		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ), sortMode, fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc(), sortMode, fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc(), sortMode, fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
						dataSet.emptyDoc1Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	void missingValue_explicit(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTestParametersWork( fieldStructure, fieldType, sortMode );

		DataSet<F> dataSet;
		SearchQuery<DocumentReference> query;

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		// Explicit order with missing().last()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc().missing().last(), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().last(), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
						dataSet.emptyDoc1Id );

		// Explicit order with missing().first()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc().missing().first(), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().first(), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );

		// Explicit order with missing().lowest()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc().missing().lowest(), sortMode,
				fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().lowest(), sortMode,
				fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
						dataSet.emptyDoc1Id );

		// Explicit order with missing().highest()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc().missing().highest(), sortMode,
				fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().highest(), sortMode,
				fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );

		// Explicit order with missing().use( ... )
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldType ) ), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL, fieldType ) ), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.emptyDoc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL, fieldType ) ), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.emptyDoc1Id,
						dataSet.doc3Id );
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL, fieldType ) ), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3254")
	void missingValue_explicit_multipleEmpty(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTestParametersWork( fieldStructure, fieldType, sortMode );

		DataSet<F> dataSet = dataSetForAsc; // We're only using ascending order
		List<DocumentReference> docRefHits;
		String fieldPath = getFieldPath( fieldStructure, fieldType );

		// using before 1 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldType ) ), sortMode,
				fieldStructure
		)
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinals( 0, 1, 2, 3 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.emptyDoc2Id,
						dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 4 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinal( 5 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using between 1 and 2 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL, fieldType ) ), sortMode,
				fieldStructure
		)
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinals( 1, 2, 3, 4 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.emptyDoc2Id,
						dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 5 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using between 2 and 3 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL, fieldType ) ), sortMode,
				fieldStructure
		)
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinal( 1 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinals( 2, 3, 4, 5 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.emptyDoc2Id,
						dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using after 3 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL, fieldType ) ), sortMode,
				fieldStructure
		).fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinal( 1 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinal( 2 ).isDocRefHit( index.typeName(), dataSet.doc3Id );
		assertThatHits( docRefHits ).ordinals( 3, 4, 5, 6 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.emptyDoc2Id,
						dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3254")
	void missingValue_multipleEmpty_useExistingDocumentValue(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTestParametersWork( fieldStructure, fieldType, sortMode );

		DataSet<F> dataSet = dataSetForAsc; // We're only using ascending order
		List<DocumentReference> docRefHits;
		String fieldPath = getFieldPath( fieldStructure, fieldType );

		Object docValue1 = getSingleValueForMissingUse( DOCUMENT_1_ORDINAL, fieldType );
		Object docValue2 = getSingleValueForMissingUse( DOCUMENT_2_ORDINAL, fieldType );
		Object docValue3 = getSingleValueForMissingUse( DOCUMENT_3_ORDINAL, fieldType );

		// using doc 1 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( docValue1 ), sortMode, fieldStructure )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinals( 0, 1, 2, 3, 4 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.doc1Id, dataSet.emptyDoc1Id,
						dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 5 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using doc 2 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( docValue2 ), sortMode, fieldStructure )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinals( 1, 2, 3, 4, 5 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.doc2Id, dataSet.emptyDoc1Id,
						dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
		assertThatHits( docRefHits ).ordinal( 6 ).isDocRefHit( index.typeName(), dataSet.doc3Id );

		// using doc 3 value
		docRefHits = matchAllQuery( dataSet, f -> f.field( fieldPath ).asc()
				.missing().use( docValue3 ), sortMode, fieldStructure )
				.fetchAllHits();
		assertThatHits( docRefHits ).ordinal( 0 ).isDocRefHit( index.typeName(), dataSet.doc1Id );
		assertThatHits( docRefHits ).ordinal( 1 ).isDocRefHit( index.typeName(), dataSet.doc2Id );
		assertThatHits( docRefHits ).ordinals( 2, 3, 4, 5, 6 )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.doc3Id, dataSet.emptyDoc1Id,
						dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4162")
	void factoryWithRoot(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTestParametersWork( fieldStructure, fieldType, sortMode );

		AbstractObjectBinding parentObjectBinding = index.binding().getParentObject( fieldStructure );

		assumeTrue(
				parentObjectBinding.absolutePath != null,
				"This test is only relevant when the field is located on an object field"
		);

		DataSet<F> dataSet = dataSetForAsc;
		assertThatQuery( index.query()
				.where( f -> f.matchAll().except( f.id().matchingAny( Arrays.asList(
						dataSet.emptyDoc1Id, dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id
				) ) ) )
				.routing( dataSet.routingKey )
				.sort( ( (Function<SearchSortFactory,
						FieldSortOptionsStep<?, ?>>) f -> f.withRoot( parentObjectBinding.absolutePath )
								.field( parentObjectBinding.getRelativeFieldName( fieldStructure, fieldType ) ) )
						.andThen( (FieldSortOptionsStep<?, ?> optionsStep1) -> applySortMode( optionsStep1, sortMode ) )
						// Don't call this.applyFilter: we need to use the relative name of the discriminator field.
						.andThen( optionsStep -> {
							if ( fieldStructure.isInNested() ) {
								return optionsStep.filter( f -> f.match()
										.field( AbstractObjectBinding.DISCRIMINATOR_FIELD_NAME )
										.matching( "included" ) );
							}
							else {
								return optionsStep;
							}
						} ) ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = { "HSEARCH-4513" })
	void concurrentQueriesUsingSameSort(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTestParametersWork( fieldStructure, fieldType, sortMode );

		DataSet<F> dataSet;
		String fieldPath = getFieldPath( fieldStructure, fieldType );

		StubMappingScope scope = index.createScope();

		SearchSort sort = applyFilter( applySortMode( scope.sort().field( fieldPath ), sortMode ), fieldStructure ).toSort();

		dataSet = dataSetForAsc;
		SearchQuery<DocumentReference> query1 = scope.query()
				.where( f -> f.id().matchingAny( Arrays.asList( dataSet.doc1Id, dataSet.doc2Id ) ) )
				.routing( dataSet.routingKey )
				// Reuse the same sort in multiple queries
				.sort( sort )
				.toQuery();
		SearchQuery<DocumentReference> query2 = scope.query()
				.where( f -> f.id().matching( "NOT_MATCHING_ANYTHING" ) )
				.routing( dataSet.routingKey )
				// Reuse the same sort in multiple queries
				.sort( sort )
				.toQuery();
		assertThatQuery( query1 )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id );
		assertThatQuery( query2 )
				.hasNoHits();
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = { "HSEARCH-4513" })
	void missingValue_multipleOptionsSameTime(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F, ?> fieldType, SortMode sortMode,
			DataSet<F> dataSetForAsc, DataSet<F> dataSetForDesc) {
		assumeTestParametersWork( fieldStructure, fieldType, sortMode );

		DataSet<F> dataSet;
		SearchQuery<DocumentReference> query;

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		// Explicit order with missing().last()
		dataSet = dataSetForAsc;
		query = matchNonEmptyAndEmpty1Query( dataSet,
				f -> f.field( fieldPath ).asc().missing().last().missing().lowest().missing().first(), sortMode,
				fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc().missing().first().missing().highest(),
				sortMode,
				fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );
		dataSet = dataSetForDesc;
		query = matchNonEmptyAndEmpty1Query( dataSet, f -> f.field( fieldPath ).desc()
				.missing().first()
				.missing().highest()
				.missing().last()
				.missing().lowest(), sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
						dataSet.emptyDoc1Id );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor,
			SortMode sortMode, TestedFieldStructure fieldStructure) {
		return matchNonEmptyQuery( dataSet, sortContributor, index.createScope(), sortMode, fieldStructure );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor, StubMappingScope scope,
			SortMode sortMode, TestedFieldStructure fieldStructure) {
		return query(
				dataSet,
				f -> f.matchAll()
						.except( f.id()
								.matchingAny( Arrays.asList( dataSet.emptyDoc1Id, dataSet.emptyDoc2Id, dataSet.emptyDoc3Id,
										dataSet.emptyDoc4Id ) ) ),
				sortContributor,
				scope,
				sortMode,
				fieldStructure
		);
	}

	private SearchQuery<DocumentReference> matchNonEmptyAndEmpty1Query(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor,
			SortMode sortMode, TestedFieldStructure fieldStructure) {
		return matchNonEmptyAndEmpty1Query( dataSet, sortContributor, index.createScope(), sortMode, fieldStructure );
	}

	private SearchQuery<DocumentReference> matchNonEmptyAndEmpty1Query(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor, StubMappingScope scope,
			SortMode sortMode, TestedFieldStructure fieldStructure) {
		return query(
				dataSet,
				f -> f.matchAll().except(
						f.id().matchingAny( Arrays.asList( dataSet.emptyDoc2Id, dataSet.emptyDoc3Id, dataSet.emptyDoc4Id ) ) ),
				sortContributor,
				scope,
				sortMode,
				fieldStructure
		);
	}

	private SearchQuery<DocumentReference> matchAllQuery(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor,
			SortMode sortMode, TestedFieldStructure fieldStructure) {
		return matchAllQuery( dataSet, sortContributor, index.createScope(), sortMode, fieldStructure );
	}

	private SearchQuery<DocumentReference> matchAllQuery(DataSet<F> dataSet,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor, StubMappingScope scope,
			SortMode sortMode, TestedFieldStructure fieldStructure) {
		return query( dataSet, f -> f.matchAll(), sortContributor, scope, sortMode, fieldStructure );
	}

	private SearchQuery<DocumentReference> query(DataSet<F> dataSet,
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor,
			Function<? super SearchSortFactory, ? extends FieldSortOptionsStep<?, ?>> sortContributor,
			StubMappingScope scope, SortMode sortMode, TestedFieldStructure fieldStructure) {
		return scope.query()
				.where( predicateContributor )
				.routing( dataSet.routingKey )
				.sort( sortContributor.andThen( t -> applySortMode( t, sortMode ) )
						.andThen( t -> applyFilter( t, fieldStructure ) ) )
				.toQuery();
	}

	private FieldSortOptionsStep<?, ?> applySortMode(FieldSortOptionsStep<?, ?> optionsStep, SortMode sortMode) {
		if ( sortMode != null ) {
			return optionsStep.mode( sortMode );
		}
		else {
			return optionsStep;
		}
	}

	private FieldSortOptionsStep<?, ?> applyFilter(FieldSortOptionsStep<?, ?> optionsStep,
			TestedFieldStructure fieldStructure) {
		if ( fieldStructure.isInNested() ) {
			return optionsStep.filter( f -> f.match()
					.field( index.binding().getDiscriminatorFieldPath( fieldStructure ) )
					.matching( "included" ) );
		}
		else {
			return optionsStep;
		}
	}

	private void assumeTestParametersWork(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType,
			SortMode sortMode) {
		assumeFalse(
				isMedianWithNestedField( sortMode, fieldStructure )
						|| isSumOrAvgOrMedianWithStringField( sortMode, fieldType )
						|| isSumWithTemporalField( sortMode, fieldType ),
				"This combination is not expected to work"
		);
	}

	private boolean isSumOrAvgOrMedianWithStringField(SortMode sortMode, FieldTypeDescriptor<F, ?> fieldType) {
		return EnumSet.of( SortMode.SUM, SortMode.AVG, SortMode.MEDIAN ).contains( sortMode )
				&& String.class.equals( fieldType.getJavaType() );
	}

	private boolean isSumWithTemporalField(SortMode sortMode, FieldTypeDescriptor<F, ?> fieldType) {
		return SortMode.SUM.equals( sortMode )
				&& ( Temporal.class.isAssignableFrom( fieldType.getJavaType() )
						|| MonthDay.class.equals( fieldType.getJavaType() ) );
	}

	private boolean isMedianWithNestedField(SortMode sortMode, TestedFieldStructure fieldStructure) {
		return SortMode.MEDIAN.equals( sortMode )
				&& fieldStructure.isInNested();
	}

	private String getFieldPath(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType) {
		return index.binding().getFieldPath( fieldStructure, fieldType );
	}

	@SuppressWarnings("unchecked")
	private F getSingleValueForMissingUse(int ordinal, FieldTypeDescriptor<F, ?> fieldType) {
		F value = fieldType.getAscendingUniqueTermValues().getSingle().get( ordinal );

		if ( fieldType instanceof NormalizedStringFieldTypeDescriptor
				&& !TckConfiguration.get().getBackendFeatures().normalizesStringMissingValues() ) {
			// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
			// TODO HSEARCH-3387 Remove this once all backends correctly normalize missing value replacements
			value = (F) ( (String) value ).toLowerCase( Locale.ROOT );
		}

		return value;
	}

	private static class DataSet<F> {
		private final TestedFieldStructure fieldStructure;
		private final FieldTypeDescriptor<F, ?> fieldType;
		private final SortMode expectedSortMode;
		private final String routingKey;

		private final String doc1Id;
		private final String doc2Id;
		private final String doc3Id;

		private final String emptyDoc1Id;
		private final String emptyDoc2Id;
		private final String emptyDoc3Id;
		private final String emptyDoc4Id;

		private DataSet(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, SortMode sortModeOrNull,
				SortMode expectedSortMode) {
			this.fieldStructure = fieldStructure;
			this.fieldType = fieldType;
			this.expectedSortMode = expectedSortMode;
			this.routingKey = fieldType.getUniqueName() + "_" + fieldStructure.getUniqueName()
					+ "_" + sortModeOrNull + "_" + expectedSortMode;
			this.doc1Id = docId( 1 );
			this.doc2Id = docId( 2 );
			this.doc3Id = docId( 3 );
			this.emptyDoc1Id = emptyDocId( 1 );
			this.emptyDoc2Id = emptyDocId( 2 );
			this.emptyDoc3Id = emptyDocId( 3 );
			this.emptyDoc4Id = emptyDocId( 4 );
		}

		private String docId(int docNumber) {
			return routingKey + "_doc_" + docNumber;
		}

		private String emptyDocId(int docNumber) {
			return routingKey + "_emptyDoc_" + docNumber;
		}

		private void contribute(BulkIndexer indexer) {
			if ( fieldStructure.isSingleValued() ) {
				List<F> values = fieldType.getAscendingUniqueTermValues().getSingle();
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				indexer.add( documentProvider( emptyDoc4Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc2Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_2_ORDINAL ), values.get( DOCUMENT_3_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_1_ORDINAL ), values.get( DOCUMENT_2_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc2Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc3Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_3_ORDINAL ), values.get( DOCUMENT_1_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc3Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
			}
			else {
				List<List<F>> values = fieldType.getAscendingUniqueTermValues()
						.getMultiResultingInSingle( expectedSortMode );
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				indexer.add( documentProvider( emptyDoc4Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc2Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_2_ORDINAL ), values.get( DOCUMENT_3_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc1Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_1_ORDINAL ), values.get( DOCUMENT_2_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc2Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
				indexer.add( documentProvider( doc3Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_3_ORDINAL ), values.get( DOCUMENT_1_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc3Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
			}
		}
	}

}
