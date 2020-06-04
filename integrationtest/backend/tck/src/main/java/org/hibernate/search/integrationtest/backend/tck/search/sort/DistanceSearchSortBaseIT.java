/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static java.util.Arrays.asList;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.Assertions;

/**
 * Tests basic behavior of sorts by distance.
 */
@RunWith(Parameterized.class)
public class DistanceSearchSortBaseIT {

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

	private static final GeoPoint CENTER_POINT = GeoPoint.of( 46.038673, 3.978563 );

	// TODO HSEARCH-3863 use the other ordinals when we implement.missing().use/last/first for distance sorts
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
			root -> new SingleFieldIndexBinding( root, supportedFieldTypes, c -> c.sortable( Sortable.YES ) );

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

	public DistanceSearchSortBaseIT(TestedFieldStructure fieldStructure, SortMode sortMode,
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
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );

		dataSet = dataSetForAsc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.doc1Id, dataSet.doc2Id, dataSet.doc3Id, dataSet.emptyDoc1Id );

		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id );

		dataSet = dataSetForDesc;
		query = simpleQuery(
				dataSet,
				b -> b.distance( fieldPath, CENTER_POINT.latitude(), CENTER_POINT.longitude() )
						.desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.emptyDoc1Id, dataSet.doc3Id, dataSet.doc2Id, dataSet.doc1Id );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void medianWithNestedField() {
		Assume.assumeTrue(
				"This test is only relevant when using SortMode.MEDIAN in nested fields",
				isMedianWithNestedField()
		);

		String fieldPath = getFieldPath();

		Assertions.assertThatThrownBy( () -> simpleQuery( dataSetForAsc, b -> b.distance( fieldPath, CENTER_POINT ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot compute the median across nested documents",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void sum() {
		Assume.assumeTrue(
				"This test is only relevant when using SortMode.SUM",
				isSum()
		);

		String fieldPath = getFieldPath();

		Assertions.assertThatThrownBy( () -> simpleQuery( dataSetForAsc, b -> b.distance( fieldPath, CENTER_POINT ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot compute the sum for a distance sort",
						"Only min, max, avg and median are supported",
						fieldPath
				);
	}

	private void assumeTestParametersWork() {
		Assume.assumeFalse(
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
		StubMappingScope scope = index.createScope();
		return scope.query()
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

	private static class AscendingUniqueDistanceFromCenterValues extends AscendingUniqueTermValues<GeoPoint> {
		private static final AscendingUniqueDistanceFromCenterValues INSTANCE = new AscendingUniqueDistanceFromCenterValues();

		@Override
		protected List<GeoPoint> createSingle() {
			return asList(
					CENTER_POINT, // ~0km
					GeoPoint.of( 46.038683, 3.964652 ), // ~1km
					GeoPoint.of( 46.059852, 3.978235 ), // ~2km
					GeoPoint.of( 46.039763, 3.914977 ), // ~4km
					GeoPoint.of( 46.000833, 3.931265 ), // ~6km
					GeoPoint.of( 46.094712, 4.044507 ), // ~8km
					GeoPoint.of( 46.018378, 4.196792 ), // ~10km
					GeoPoint.of( 46.123025, 3.845305 ) // ~14km
			);
		}

		@Override
		protected List<List<GeoPoint>> createMultiResultingInSingleAfterSum() {
			return valuesThatWontBeUsed();
		}

		@Override
		protected List<List<GeoPoint>> createMultiResultingInSingleAfterAvg() {
			return asList(
					asList( CENTER_POINT, CENTER_POINT ), // ~0km
					asList( getSingle().get( 0 ), getSingle().get( 2 ) ), // ~1km
					asList( getSingle().get( 1 ), getSingle().get( 1 ), getSingle().get( 4 ) ), // ~2km
					asList( getSingle().get( 2 ), getSingle().get( 4 ) ), // ~4km
					asList( getSingle().get( 3 ), getSingle().get( 5 ) ), // ~6km
					asList( getSingle().get( 4 ), getSingle().get( 6 ) ), // ~8km
					asList( getSingle().get( 4 ), getSingle().get( 7 ) ), // ~10km
					asList( getSingle().get( 7 ), getSingle().get( 7 ), getSingle().get( 7 ) ) // ~14km
			);
		}
	}

}
