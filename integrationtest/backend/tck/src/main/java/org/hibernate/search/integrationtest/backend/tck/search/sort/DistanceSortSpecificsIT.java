/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues.CENTER_POINT;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.AbstractObjectBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
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
 * Tests specifics of sorts by distance.
 */

class DistanceSortSpecificsIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;
	private static final Set<StandardFieldTypeDescriptor<GeoPoint>> supportedFieldTypes = Collections.singleton( fieldType );
	private static final List<DataSet> dataSets = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			// We need two separate datasets when the sort mode is not defined,
			// because then the sort mode will be inferred automatically to
			// MIN for desc order, or MAX for asc order.
			DataSet dataSetForAsc = new DataSet( fieldStructure, null, SortMode.MIN );
			dataSets.add( dataSetForAsc );
			DataSet dataSetForDesc = new DataSet( fieldStructure, null, SortMode.MAX );
			dataSets.add( dataSetForDesc );
			parameters.add( Arguments.of( fieldStructure, null, dataSetForAsc, dataSetForDesc ) );
			for ( SortMode sortMode : SortMode.values() ) {
				// When the sort mode is defined, we only need one dataset.
				dataSetForAsc = new DataSet( fieldStructure, sortMode, sortMode );
				dataSets.add( dataSetForAsc );
				dataSetForDesc = dataSetForAsc;
				parameters.add( Arguments.of( fieldStructure, sortMode, dataSetForAsc, dataSetForDesc ) );
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
		for ( DataSet dataSet : dataSets ) {
			dataSet.contribute( indexer );
		}
		indexer.join();
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	void simple(TestedFieldStructure fieldStructure, SortMode sortMode,
			DataSet dataSetForAsc, DataSet dataSetForDesc) {
		assumeTestParametersWork( sortMode, fieldStructure );

		DataSet dataSet;
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath( fieldStructure );

		dataSet = dataSetForAsc;
		query = simpleQuery( dataSet, b -> b.distance( fieldPath, CENTER_POINT ), sortMode, fieldStructure );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() ),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc(),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT ).desc(),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );

		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.desc(),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	void missingValue_explicit(TestedFieldStructure fieldStructure, SortMode sortMode,
			DataSet dataSetForAsc, DataSet dataSetForDesc) {
		assumeTestParametersWork( sortMode, fieldStructure );

		DataSet dataSet;
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath( fieldStructure );

		// Explicit order with missing().last()
		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().last(),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		if ( !TckConfiguration.get().getBackendFeatures().geoDistanceSortingSupportsConfigurableMissingValues() ) {
			assertThatThrownBy( () -> simpleQuery(
					dataSetForDesc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.desc().missing().last(),
					sortMode,
					fieldStructure
			) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid use of 'missing().last()' for a descending distance sort.",
							"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized." );
		}
		else {
			dataSet = dataSetForDesc;
			query = simpleQuery(
					dataSet,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.desc().missing().last(),
					sortMode,
					fieldStructure
			);
			assertThatQuery( query )
					.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
							dataSet.emptyDoc1Id );
		}

		// Explicit order with missing().lowest()
		if ( !TckConfiguration.get().getBackendFeatures().geoDistanceSortingSupportsConfigurableMissingValues() ) {
			assertThatThrownBy( () -> simpleQuery(
					dataSetForDesc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.desc().missing().lowest(),
					sortMode,
					fieldStructure
			) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid use of 'missing().lowest()' for a descending distance sort.",
							"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized." );

			assertThatThrownBy( () -> simpleQuery(
					dataSetForDesc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.asc().missing().lowest(),
					sortMode,
					fieldStructure
			) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid use of 'missing().lowest()' for an ascending distance sort.",
							"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized." );
		}
		else {
			dataSet = dataSetForAsc;
			query = simpleQuery(
					dataSet,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.asc().missing().lowest(),
					sortMode,
					fieldStructure
			);
			assertThatQuery( query )
					.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
							dataSet.doc3Id );

			dataSet = dataSetForDesc;
			query = simpleQuery(
					dataSet,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.desc().missing().lowest(),
					sortMode,
					fieldStructure
			);
			assertThatQuery( query )
					.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id,
							dataSet.emptyDoc1Id );
		}

		// Explicit order with missing().first()
		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.desc().missing().first(),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );

		if ( !TckConfiguration.get().getBackendFeatures().geoDistanceSortingSupportsConfigurableMissingValues() ) {
			// explicit .asc()
			assertThatThrownBy( () -> simpleQuery(
					dataSetForAsc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.asc().missing().first(),
					sortMode,
					fieldStructure
			) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid use of 'missing().first()' for an ascending distance sort.",
							"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized." );

			// implicit .asc()
			assertThatThrownBy( () -> simpleQuery(
					dataSetForAsc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.missing().first(),
					sortMode,
					fieldStructure
			) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid use of 'missing().first()' for an ascending distance sort.",
							"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized." );
		}
		else {
			dataSet = dataSetForAsc;
			query = simpleQuery(
					dataSet,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.asc().missing().first(),
					sortMode,
					fieldStructure
			);
			assertThatQuery( query )
					.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
							dataSet.doc3Id );
		}

		// Explicit order with missing().highest()
		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.desc().missing().highest(),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id,
						dataSet.doc1Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().highest(),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );

		// Explicit order with missing().use( ... )
		if ( !TckConfiguration.get().getBackendFeatures().geoDistanceSortingSupportsConfigurableMissingValues() ) {
			assertThatThrownBy( () -> simpleQuery(
					dataSetForAsc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.asc().missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ),
					sortMode,
					fieldStructure
			) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid use of 'missing().use(...)' for a distance sort.",
							"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized." );

			return;
		}

		// Backend supports missing().use( ... )
		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id,
						dataSet.doc3Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.emptyDoc1Id, dataSet.doc2Id,
						dataSet.doc3Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.emptyDoc1Id,
						dataSet.doc3Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ),
				sortMode,
				fieldStructure
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id,
						dataSet.emptyDoc1Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	void medianWithNestedField(TestedFieldStructure fieldStructure, SortMode sortMode,
			DataSet dataSetForAsc, DataSet dataSetForDesc) {
		assumeTrue(
				isMedianWithNestedField( sortMode, fieldStructure ),
				"This test is only relevant when using SortMode.MEDIAN in nested fields"
		);

		String fieldPath = getFieldPath( fieldStructure );

		assertThatThrownBy( () -> simpleQuery( dataSetForAsc, b -> b.distance( fieldPath, CENTER_POINT ), sortMode,
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
	void sum(TestedFieldStructure fieldStructure, SortMode sortMode,
			DataSet dataSetForAsc, DataSet dataSetForDesc) {
		assumeTrue(
				isSum( sortMode ),
				"This test is only relevant when using SortMode.SUM"
		);

		String fieldPath = getFieldPath( fieldStructure );

		assertThatThrownBy( () -> simpleQuery( dataSetForAsc, b -> b.distance( fieldPath, CENTER_POINT ), sortMode,
				fieldStructure
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: SUM. This sort mode is not supported for a distance sort",
						"Only MIN, MAX, AVG and MEDIAN are supported",
						fieldPath
				);
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4162")
	void factoryWithRoot(TestedFieldStructure fieldStructure, SortMode sortMode,
			DataSet dataSetForAsc, DataSet dataSetForDesc) {
		assumeTestParametersWork( sortMode, fieldStructure );

		AbstractObjectBinding parentObjectBinding = index.binding().getParentObject( fieldStructure );

		assumeTrue(
				parentObjectBinding.absolutePath != null,
				"This test is only relevant when the field is located on an object field"
		);

		DataSet dataSet = dataSetForAsc;
		assertThatQuery( index.query()
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.sort( ( (Function<SearchSortFactory,
						DistanceSortOptionsStep<?, ?>>) f -> f.withRoot( parentObjectBinding.absolutePath )
								.distance( parentObjectBinding.getRelativeFieldName( fieldStructure, fieldType ),
										CENTER_POINT ) )
						.andThen( (DistanceSortOptionsStep<?, ?> optionsStep1) -> applySortMode( optionsStep1, sortMode
						) )
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
				.hasDocRefHitsExactOrder( index.typeName(),
						dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );
	}

	@ParameterizedTest(name = "{0} - {2} - {1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = { "HSEARCH-4513" })
	void concurrentQueriesUsingSameSort(TestedFieldStructure fieldStructure, SortMode sortMode,
			DataSet dataSetForAsc, DataSet dataSetForDesc) {
		assumeTestParametersWork( sortMode, fieldStructure );

		DataSet dataSet;
		String fieldPath = getFieldPath( fieldStructure );

		StubMappingScope scope = index.createScope();

		SearchSort sort = applyFilter( applySortMode( scope.sort().distance( fieldPath, CENTER_POINT ), sortMode ),
				fieldStructure
		).toSort();

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

	private void assumeTestParametersWork(SortMode sortMode, TestedFieldStructure fieldStructure) {
		assumeFalse(
				isMedianWithNestedField( sortMode, fieldStructure ) || isSum( sortMode ),
				"This combination is not expected to work"
		);
	}

	private boolean isMedianWithNestedField(SortMode sortMode, TestedFieldStructure fieldStructure) {
		return SortMode.MEDIAN.equals( sortMode ) && fieldStructure.isInNested();
	}

	private boolean isSum(SortMode sortMode) {
		return SortMode.SUM.equals( sortMode );
	}

	private SearchQuery<DocumentReference> simpleQuery(DataSet dataSet,
			Function<? super SearchSortFactory, ? extends DistanceSortOptionsStep<?, ?>> sortContributor,
			SortMode sortMode, TestedFieldStructure fieldStructure) {
		return index.query()
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.sort( sortContributor.andThen( t -> applySortMode( t, sortMode ) )
						.andThen( t -> applyFilter( t, fieldStructure ) ) )
				.toQuery();
	}

	private DistanceSortOptionsStep<?, ?> applySortMode(DistanceSortOptionsStep<?, ?> optionsStep, SortMode sortMode) {
		if ( sortMode != null ) {
			return optionsStep.mode( sortMode );
		}
		else {
			return optionsStep;
		}
	}

	private DistanceSortOptionsStep<?, ?> applyFilter(DistanceSortOptionsStep<?, ?> optionsStep,
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

	private String getFieldPath(TestedFieldStructure fieldStructure) {
		return index.binding().getFieldPath( fieldStructure, fieldType );
	}

	private GeoPoint getSingleValueForMissingUse(int ordinal) {
		return AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingle().get( ordinal );
	}

	private static class DataSet {
		private final TestedFieldStructure fieldStructure;
		private final SortMode expectedSortMode;
		private final String routingKey;

		private final String doc1Id;
		private final String doc2Id;
		private final String doc3Id;

		private final String emptyDoc1Id;

		private DataSet(TestedFieldStructure fieldStructure, SortMode sortModeOrNull,
				SortMode expectedSortMode) {
			this.fieldStructure = fieldStructure;
			this.expectedSortMode = expectedSortMode;
			this.routingKey = fieldStructure.getUniqueName() + "_" + sortModeOrNull + "_" + expectedSortMode;
			this.doc1Id = docId( 1 );
			this.doc2Id = docId( 2 );
			this.doc3Id = docId( 3 );
			this.emptyDoc1Id = emptyDocId( 1 );
		}

		private String docId(int docNumber) {
			return routingKey + "_doc_" + docNumber;
		}

		private String emptyDocId(int docNumber) {
			return routingKey + "_emptyDoc_" + docNumber;
		}

		private void contribute(BulkIndexer indexer) {
			if ( fieldStructure.isSingleValued() ) {
				List<GeoPoint> values = AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingle();
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				indexer.add( documentProvider( doc2Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_2_ORDINAL ), values.get( DOCUMENT_3_ORDINAL ) ) ) );
				indexer.add( documentProvider( doc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_1_ORDINAL ), values.get( DOCUMENT_2_ORDINAL ) ) ) );
				indexer.add( documentProvider( doc3Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_3_ORDINAL ), values.get( DOCUMENT_1_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
			}
			else {
				List<List<GeoPoint>> values = AscendingUniqueDistanceFromCenterValues.INSTANCE
						.getMultiResultingInSingle( expectedSortMode );
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				indexer.add( documentProvider( doc2Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_2_ORDINAL ), values.get( DOCUMENT_3_ORDINAL ) ) ) );
				indexer.add( documentProvider( doc1Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_1_ORDINAL ), values.get( DOCUMENT_2_ORDINAL ) ) ) );
				indexer.add( documentProvider( doc3Id, routingKey,
						document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
								document, values.get( DOCUMENT_3_ORDINAL ), values.get( DOCUMENT_1_ORDINAL ) ) ) );
				indexer.add( documentProvider( emptyDoc1Id, routingKey,
						document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null, null ) ) );
			}
		}
	}
}
