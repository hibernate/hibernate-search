/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues.CENTER_POINT;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

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
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests basic behavior of sorts by distance.
 */
@RunWith(Parameterized.class)
public class DistanceSortBaseIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;
	private static final Set<FieldTypeDescriptor<GeoPoint>> supportedFieldTypes = Collections.singleton( fieldType );
	private static List<DataSet> dataSets;

	@Parameterized.Parameters(name = "{0} - {2} - {1}")
	public static Object[][] parameters() {
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			// We need two separate datasets when the sort mode is not defined,
			// because then the sort mode will be inferred automatically to
			// MIN for desc order, or MAX for asc order.
			DataSet dataSetForAsc = new DataSet( fieldStructure, null, SortMode.MIN );
			dataSets.add( dataSetForAsc );
			DataSet dataSetForDesc = new DataSet( fieldStructure, null, SortMode.MAX );
			dataSets.add( dataSetForDesc );
			parameters.add( new Object[] { fieldStructure, null, dataSetForAsc, dataSetForDesc } );
			for ( SortMode sortMode : SortMode.values() ) {
				// When the sort mode is defined, we only need one dataset.
				dataSetForAsc = new DataSet( fieldStructure, sortMode, sortMode );
				dataSets.add( dataSetForAsc );
				dataSetForDesc = dataSetForAsc;
				parameters.add( new Object[] { fieldStructure, sortMode, dataSetForAsc, dataSetForDesc } );
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	private static final int BEFORE_DOCUMENT_1_ORDINAL = 0;
	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int BETWEEN_DOCUMENT_1_AND_2_ORDINAL = 2;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int BETWEEN_DOCUMENT_2_AND_3_ORDINAL = 4;
	private static final int DOCUMENT_3_ORDINAL = 5;
	private static final int AFTER_DOCUMENT_3_ORDINAL = 6;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final Function<IndexSchemaElement, SingleFieldIndexBinding> bindingFactory =
			root -> SingleFieldIndexBinding.create( root, supportedFieldTypes, c -> c.sortable( Sortable.YES ) );

	private static final SimpleMappedIndex<SingleFieldIndexBinding> index = SimpleMappedIndex.of( bindingFactory );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		for ( DataSet dataSet : dataSets ) {
			dataSet.contribute( indexer );
		}
		indexer.join();
	}

	private final TestedFieldStructure fieldStructure;
	private final SortMode sortMode;
	private final DataSet dataSetForAsc;
	private final DataSet dataSetForDesc;

	public DistanceSortBaseIT(TestedFieldStructure fieldStructure, SortMode sortMode,
			DataSet dataSetForAsc, DataSet dataSetForDesc) {
		this.fieldStructure = fieldStructure;
		this.sortMode = sortMode;
		this.dataSetForAsc = dataSetForAsc;
		this.dataSetForDesc = dataSetForDesc;
	}

	@Test
	public void simple() {
		assumeTestParametersWork();

		DataSet dataSet;
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		dataSet = dataSetForAsc;
		query = simpleQuery( dataSet, b -> b.distance( fieldPath, CENTER_POINT ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc()
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );

		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT ).desc()
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id );

		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.desc()
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id );
	}

	@Test
	public void missingValue_explicit() {
		assumeTestParametersWork();

		DataSet dataSet;
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		// Explicit order with missing().last()
		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().last()
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );

		if ( !TckConfiguration.get().getBackendFeatures().geoDistanceSortingSupportsConfigurableMissingValues() ) {
			assertThatThrownBy( () -> simpleQuery(
					dataSetForDesc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.desc().missing().last()
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
							.desc().missing().last()
			);
			assertThatQuery( query )
					.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id, dataSet.emptyDoc1Id );
		}

		// Explicit order with missing().lowest()
		if ( !TckConfiguration.get().getBackendFeatures().geoDistanceSortingSupportsConfigurableMissingValues() ) {
			assertThatThrownBy( () -> simpleQuery(
					dataSetForDesc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.desc().missing().lowest()
			) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid use of 'missing().lowest()' for a descending distance sort.",
							"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized." );

			assertThatThrownBy( () -> simpleQuery(
					dataSetForDesc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.asc().missing().lowest()
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
							.asc().missing().lowest()
			);
			assertThatQuery( query )
					.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );

			dataSet = dataSetForDesc;
			query = simpleQuery(
					dataSet,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.desc().missing().lowest()
			);
			assertThatQuery( query )
					.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id, dataSet.emptyDoc1Id );
		}

		// Explicit order with missing().first()
		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.desc().missing().first()
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id );

		if ( !TckConfiguration.get().getBackendFeatures().geoDistanceSortingSupportsConfigurableMissingValues() ) {
			// explicit .asc()
			assertThatThrownBy( () -> simpleQuery(
					dataSetForAsc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.asc().missing().first()
			) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid use of 'missing().first()' for an ascending distance sort.",
							"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized." );

			// implicit .asc()
			assertThatThrownBy( () -> simpleQuery(
					dataSetForAsc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.missing().first()
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
							.asc().missing().first()
			);
			assertThatQuery( query )
					.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );
		}

		// Explicit order with missing().highest()
		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.desc().missing().highest()
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().highest()
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );

		// Explicit order with missing().use( ... )
		if ( !TckConfiguration.get().getBackendFeatures().geoDistanceSortingSupportsConfigurableMissingValues() ) {
			assertThatThrownBy( () -> simpleQuery(
					dataSetForAsc,
					b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
							.asc().missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) )
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
						.asc().missing().use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) )
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) )
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.emptyDoc1Id, dataSet.doc2Id, dataSet.doc3Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) )
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.emptyDoc1Id, dataSet.doc3Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc().missing().use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) )
		);
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void medianWithNestedField() {
		assumeTrue(
				"This test is only relevant when using SortMode.MEDIAN in nested fields",
				isMedianWithNestedField()
		);

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> simpleQuery( dataSetForAsc, b -> b.distance( fieldPath, CENTER_POINT ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: MEDIAN",
						"This sort mode is not supported for fields in nested documents",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void sum() {
		assumeTrue(
				"This test is only relevant when using SortMode.SUM",
				isSum()
		);

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> simpleQuery( dataSetForAsc, b -> b.distance( fieldPath, CENTER_POINT ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid sort mode: SUM. This sort mode is not supported for a distance sort",
						"Only MIN, MAX, AVG and MEDIAN are supported",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void factoryWithRoot() {
		assumeTestParametersWork();

		AbstractObjectBinding parentObjectBinding = index.binding().getParentObject( fieldStructure );

		assumeTrue( "This test is only relevant when the field is located on an object field",
				parentObjectBinding.absolutePath != null );

		DataSet dataSet = dataSetForAsc;
		assertThatQuery( index.query()
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.sort( ( (Function<SearchSortFactory, DistanceSortOptionsStep<?, ?>>)
						f -> f.withRoot( parentObjectBinding.absolutePath )
								.distance( parentObjectBinding.getRelativeFieldName( fieldStructure, fieldType ), CENTER_POINT ) )
						.andThen( this::applySortMode )
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

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-4513" })
	public void concurrentQueriesUsingSameSort() {
		assumeTestParametersWork();

		DataSet dataSet;
		String fieldPath = getFieldPath();

		StubMappingScope scope = index.createScope();

		SearchSort sort = applyFilter( applySortMode( scope.sort().distance( fieldPath, CENTER_POINT ) ) ).toSort();

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

	private void assumeTestParametersWork() {
		assumeFalse(
				"This combination is not expected to work",
				isMedianWithNestedField() || isSum()
		);
	}

	private boolean isMedianWithNestedField() {
		return SortMode.MEDIAN.equals( sortMode ) && fieldStructure.isInNested();
	}

	private boolean isSum() {
		return SortMode.SUM.equals( sortMode );
	}

	private SearchQuery<DocumentReference> simpleQuery(DataSet dataSet,
			Function<? super SearchSortFactory, ? extends DistanceSortOptionsStep<?, ?>> sortContributor) {
		return index.query()
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.sort( sortContributor.andThen( this::applySortMode ).andThen( this::applyFilter ) )
				.toQuery();
	}

	private DistanceSortOptionsStep<?, ?> applySortMode(DistanceSortOptionsStep<?, ?> optionsStep) {
		if ( sortMode != null ) {
			return optionsStep.mode( sortMode );
		}
		else {
			return optionsStep;
		}
	}

	private DistanceSortOptionsStep<?, ?> applyFilter(DistanceSortOptionsStep<?, ?> optionsStep) {
		if ( fieldStructure.isInNested() ) {
			return optionsStep.filter( f -> f.match()
					.field( index.binding().getDiscriminatorFieldPath( fieldStructure ) )
					.matching( "included" ) );
		}
		else {
			return optionsStep;
		}
	}

	private String getFieldPath() {
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
