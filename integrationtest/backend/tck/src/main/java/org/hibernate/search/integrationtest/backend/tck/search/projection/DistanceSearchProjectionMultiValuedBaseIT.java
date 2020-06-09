/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_1;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_2;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.data.Pair;
import org.hibernate.search.util.impl.test.data.Triplet;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.ListAssert;

/**
 * Tests basic behavior of projections on the distance between a given point and the value of a multi-valued field.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3391")
public class DistanceSearchProjectionMultiValuedBaseIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;
	private static final Set<FieldTypeDescriptor<GeoPoint>> supportedFieldTypes = Collections.singleton( fieldType );
	private static List<DataSet> dataSets;

	private static final Comparator<Iterable<Double>> APPROX_M_COMPARATOR =
			TestComparators.lexicographic( TestComparators.APPROX_M_COMPARATOR );
	private static final Comparator<Iterable<Double>> APPROX_KM_COMPARATOR =
			TestComparators.lexicographic( TestComparators.APPROX_KM_COMPARATOR );
	private static final Comparator<Iterable<Double>> APPROX_MILES_COMPARATOR =
			TestComparators.lexicographic( TestComparators.APPROX_MILES_COMPARATOR );

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			if ( fieldStructure.isSingleValued() ) {
				continue;
			}
			DataSet dataSet = new DataSet( fieldStructure );
			dataSets.add( dataSet );
			parameters.add( new Object[] { fieldStructure, dataSet } );
		}
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<SingleFieldIndexBinding> mainIndex =
			SimpleMappedIndex.of( root -> SingleFieldIndexBinding.create(
					root, supportedFieldTypes,
					c -> c.projectable( Projectable.YES )
			) )
					.name( "main" );
	private static final SimpleMappedIndex<SingleFieldIndexBinding> sortableIndex =
			SimpleMappedIndex.of( root -> SingleFieldIndexBinding.create(
					root, supportedFieldTypes,
					c -> c.projectable( Projectable.YES ).sortable( Sortable.YES )
			) )
					.name( "sortable" );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( mainIndex, sortableIndex ).setup();

		BulkIndexer mainIndexer = mainIndex.bulkIndexer();
		BulkIndexer sortableIndexer = sortableIndex.bulkIndexer();
		for ( DataSet dataSet : dataSets ) {
			dataSet.contribute( mainIndexer, sortableIndexer );
		}
		mainIndexer.join( sortableIndexer );
	}

	private final TestedFieldStructure fieldStructure;
	private final DataSet dataSet;

	public DistanceSearchProjectionMultiValuedBaseIT(TestedFieldStructure fieldStructure, DataSet dataSet) {
		this.fieldStructure = fieldStructure;
		this.dataSet = dataSet;
	}

	@Test
	public void simple() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		assertThat( scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( f -> f.distance( fieldPath, CENTER_POINT_1 ).multi() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter1( 1 ),
						dataSet.getFieldDistancesFromCenter1( 2 ),
						dataSet.getFieldDistancesFromCenter1( 3 ),
						Collections.emptyList() // Empty document
				);
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void duplicated() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		ListAssert<Pair<List<Double>, List<Double>>> hitsAssert = assertThat( scope.query()
				.select( f -> f.composite(
						Pair::new,
						f.distance( fieldPath, CENTER_POINT_1 ).multi(),
						f.distance( fieldPath, CENTER_POINT_1 ).multi()
				) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs();

		hitsAssert.extracting( Pair::elem0 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter1( 1 ),
						dataSet.getFieldDistancesFromCenter1( 2 ),
						dataSet.getFieldDistancesFromCenter1( 3 ),
						Collections.emptyList() // Empty document
				);
		hitsAssert.extracting( Pair::elem1 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter1( 1 ),
						dataSet.getFieldDistancesFromCenter1( 2 ),
						dataSet.getFieldDistancesFromCenter1( 3 ),
						Collections.emptyList() // Empty document
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3618")
	public void sortable_withoutSort() {
		StubMappingScope scope = sortableIndex.createScope();

		String fieldPath = sortableIndex.binding().getFieldPath( fieldStructure, fieldType );

		assertThat( scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( f -> f.distance( fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT ).multi() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getSortableFieldDistancesFromCenter( 1 ),
						dataSet.getSortableFieldDistancesFromCenter( 2 ),
						dataSet.getSortableFieldDistancesFromCenter( 3 )
				);
	}

	/**
	 * This is relevant because projections on the distance can
	 * be optimized when also sorting by distance to the same point.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3618")
	public void sortable_withSort() {
		StubMappingScope scope = sortableIndex.createScope();

		String fieldPath = sortableIndex.binding().getFieldPath( fieldStructure, fieldType );

		assertThat( scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( f -> f.distance( fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT ).multi() )
				.where( f -> f.matchAll() )
				.sort( f -> f.distance( fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT ) )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactly( // in this order
						dataSet.getSortableFieldDistancesFromCenter( 1 ),
						dataSet.getSortableFieldDistancesFromCenter( 2 ),
						dataSet.getSortableFieldDistancesFromCenter( 3 )
				);
	}

	@Test
	public void unit_km() {
		StubMappingScope scope = mainIndex.createScope();

		assertThat( scope.query()
				.select( f -> f.distance( getFieldPath(), CENTER_POINT_1 ).multi()
						.unit( DistanceUnit.KILOMETERS ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_KM_COMPARATOR )
				.containsExactlyInAnyOrder(
						divideAll( dataSet.getFieldDistancesFromCenter1( 1 ), 1000 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 2 ), 1000 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 3 ), 1000 ),
						Collections.emptyList() // Empty document
				);
	}

	@Test
	public void unit_miles() {
		StubMappingScope scope = mainIndex.createScope();

		assertThat( scope.query()
				.select( f -> f.distance( getFieldPath(), CENTER_POINT_1 ).multi()
						.unit( DistanceUnit.MILES ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_MILES_COMPARATOR )
				.containsExactlyInAnyOrder(
						divideAll( dataSet.getFieldDistancesFromCenter1( 1 ), 1_609.344 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 2 ), 1_609.344 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 3 ), 1_609.344 ),
						Collections.emptyList() // Empty document
				);
	}

	@Test
	public void several() {
		StubMappingScope scope = mainIndex.createScope();

		ListAssert<Triplet<List<Double>, List<Double>, List<Double>>> hitsAssert = assertThat( scope.query()
				.select( f -> f.composite(
						Triplet::new,
						f.distance( getFieldPath(), CENTER_POINT_1 ).multi(),
						f.distance( getFieldPath(), CENTER_POINT_2 ).multi(),
						f.distance( getFieldPath(), CENTER_POINT_1 ).multi().unit( DistanceUnit.KILOMETERS )
				) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs();

		hitsAssert.extracting( Triplet::elem0 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter1( 1 ),
						dataSet.getFieldDistancesFromCenter1( 2 ),
						dataSet.getFieldDistancesFromCenter1( 3 ),
						Collections.emptyList() // Empty document
				);
		hitsAssert.extracting( Triplet::elem1 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistancesFromCenter2( 1 ),
						dataSet.getFieldDistancesFromCenter2( 2 ),
						dataSet.getFieldDistancesFromCenter2( 3 ),
						Collections.emptyList() // Empty document
				);
		hitsAssert.extracting( Triplet::elem2 )
				.usingElementComparator( APPROX_KM_COMPARATOR )
				.containsExactlyInAnyOrder(
						divideAll( dataSet.getFieldDistancesFromCenter1( 1 ), 1000 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 2 ), 1000 ),
						divideAll( dataSet.getFieldDistancesFromCenter1( 3 ), 1000 ),
						Collections.emptyList() // Empty document
				);
	}

	private String getFieldPath() {
		return mainIndex.binding().getFieldPath( fieldStructure, fieldType );
	}

	private static List<Double> divideAll(List<Double> distances, double denominator) {
		return distances.stream().map( v -> v / denominator ).collect( Collectors.toList() );
	}

	private static class DataSet {
		private final TestedFieldStructure fieldStructure;
		private final String routingKey;

		private DataSet(TestedFieldStructure fieldStructure) {
			this.fieldStructure = fieldStructure;
			this.routingKey = fieldStructure.getUniqueName();
		}

		private String docId(int docNumber) {
			return routingKey + "_doc_" + docNumber;
		}

		private String emptyDocId(int docNumber) {
			return routingKey + "_emptyDoc_" + docNumber;
		}

		private void contribute(BulkIndexer mainIndexer, BulkIndexer sortableIndexer) {
			mainIndexer.add( documentProvider( emptyDocId( 1 ), routingKey,
					document -> mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, Collections.emptyList() ) ) );
			mainIndexer.add( documentProvider( docId( 1 ), routingKey,
					document -> mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 1 ) ) ) );
			mainIndexer.add( documentProvider( docId( 2 ), routingKey,
					document -> mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 2 ) ) ) );
			mainIndexer.add( documentProvider( docId( 3 ), routingKey,
					document -> mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 3 ) ) ) );

			sortableIndexer.add( documentProvider( docId( 2 ), routingKey,
					document -> sortableIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getSortableFieldValues( 2 ) ) ) );
			sortableIndexer.add( documentProvider( docId( 1 ), routingKey,
					document -> sortableIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getSortableFieldValues( 1 ) ) ) );
			sortableIndexer.add( documentProvider( docId( 3 ), routingKey,
					document -> sortableIndex.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getSortableFieldValues( 3 ) ) ) );
		}

		private List<GeoPoint> getFieldValues(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getMulti().get( documentNumber - 1 );
		}

		private List<GeoPoint> getSortableFieldValues(int documentNumber) {
			return AscendingUniqueDistanceFromCenterValues.INSTANCE.getMultiResultingInSingle( SortMode.MIN )
					.get( documentNumber - 1 );
		}

		private List<Double> getFieldDistancesFromCenter1(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getMultiDistancesFromCenterPoint1()
					.get( documentNumber - 1 );
		}

		private List<Double> getFieldDistancesFromCenter2(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getMultiDistancesFromCenterPoint2()
					.get( documentNumber - 1 );
		}

		private List<Double> getSortableFieldDistancesFromCenter(int documentNumber) {
			return AscendingUniqueDistanceFromCenterValues.INSTANCE.getMultiDistancesFromCenterPointForMinDataset()
					.get( documentNumber - 1 );
		}
	}
}
