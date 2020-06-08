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
import static org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators.APPROX_KM_COMPARATOR;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators.APPROX_MILES_COMPARATOR;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators.APPROX_M_COMPARATOR;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
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
 * Tests basic behavior of projections on the distance between a field value and a given point.
 */
@RunWith(Parameterized.class)
public class DistanceSearchProjectionBaseIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;
	private static final Set<FieldTypeDescriptor<GeoPoint>> supportedFieldTypes = Collections.singleton( fieldType );
	private static List<DataSet> dataSets;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			if ( fieldStructure.isMultiValued() ) {
				// TODO HSEARCH-3391 support multi-valued projections
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
			SimpleMappedIndex.of( root -> SingleFieldIndexBinding.createWithSingleValuedNestedFields(
					root, supportedFieldTypes,
					c -> c.projectable( Projectable.YES )
			) )
					.name( "main" );
	private static final SimpleMappedIndex<SingleFieldIndexBinding> sortableIndex =
			SimpleMappedIndex.of( root -> SingleFieldIndexBinding.createWithSingleValuedNestedFields(
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

	public DistanceSearchProjectionBaseIT(TestedFieldStructure fieldStructure, DataSet dataSet) {
		this.fieldStructure = fieldStructure;
		this.dataSet = dataSet;
	}

	@Test
	public void simple() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		assertThat( scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( f -> f.distance( fieldPath, CENTER_POINT_1 ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistanceFromCenter1( 1 ),
						dataSet.getFieldDistanceFromCenter1( 2 ),
						dataSet.getFieldDistanceFromCenter1( 3 ),
						null // Empty document
				);
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@Test
	public void duplicated() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		ListAssert<Pair<Double, Double>> hitsAssert = assertThat( scope.query()
				.select( f -> f.composite(
						Pair::new,
						f.distance( fieldPath, CENTER_POINT_1 ),
						f.distance( fieldPath, CENTER_POINT_1 )
				) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs();

		hitsAssert.extracting( Pair::elem0 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistanceFromCenter1( 1 ),
						dataSet.getFieldDistanceFromCenter1( 2 ),
						dataSet.getFieldDistanceFromCenter1( 3 ),
						null
				);
		hitsAssert.extracting( Pair::elem1 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistanceFromCenter1( 1 ),
						dataSet.getFieldDistanceFromCenter1( 2 ),
						dataSet.getFieldDistanceFromCenter1( 3 ),
						null
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3618")
	public void sortable_withoutSort() {
		StubMappingScope scope = sortableIndex.createScope();

		String fieldPath = sortableIndex.binding().getFieldPath( fieldStructure, fieldType );

		assertThat( scope.query()
				// Do NOT add any additional projection here: this serves as a non-regression test for HSEARCH-3618
				.select( f -> f.distance( fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getSortableFieldDistanceFromCenter( 1 ),
						dataSet.getSortableFieldDistanceFromCenter( 2 ),
						dataSet.getSortableFieldDistanceFromCenter( 3 )
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
				.select( f -> f.distance( fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT ) )
				.where( f -> f.matchAll() )
				.sort( f -> f.distance( fieldPath, AscendingUniqueDistanceFromCenterValues.CENTER_POINT ) )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactly( // in this order
						dataSet.getSortableFieldDistanceFromCenter( 1 ),
						dataSet.getSortableFieldDistanceFromCenter( 2 ),
						dataSet.getSortableFieldDistanceFromCenter( 3 )
				);
	}

	@Test
	public void unit_km() {
		StubMappingScope scope = mainIndex.createScope();

		assertThat( scope.query()
				.select( f -> f.distance( getFieldPath(), CENTER_POINT_1 )
						.unit( DistanceUnit.KILOMETERS ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_KM_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistanceFromCenter1( 1 ) / 1000,
						dataSet.getFieldDistanceFromCenter1( 2 ) / 1000,
						dataSet.getFieldDistanceFromCenter1( 3 ) / 1000,
						null
				);
	}

	@Test
	public void unit_miles() {
		StubMappingScope scope = mainIndex.createScope();

		assertThat( scope.query()
				.select( f -> f.distance( getFieldPath(), CENTER_POINT_1 )
						.unit( DistanceUnit.MILES ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( APPROX_MILES_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistanceFromCenter1( 1 ) / 1_609.344,
						dataSet.getFieldDistanceFromCenter1( 2 ) / 1_609.344,
						dataSet.getFieldDistanceFromCenter1( 3 ) / 1_609.344,
						null
				);
	}

	@Test
	public void several() {
		StubMappingScope scope = mainIndex.createScope();

		ListAssert<Triplet<Double, Double, Double>> hitsAssert = assertThat( scope.query()
				.select( f -> f.composite(
						Triplet::new,
						f.distance( getFieldPath(), CENTER_POINT_1 ),
						f.distance( getFieldPath(), CENTER_POINT_2 ),
						f.distance( getFieldPath(), CENTER_POINT_1 ).unit( DistanceUnit.KILOMETERS )
				) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs();

		hitsAssert.extracting( Triplet::elem0 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistanceFromCenter1( 1 ),
						dataSet.getFieldDistanceFromCenter1( 2 ),
						dataSet.getFieldDistanceFromCenter1( 3 ),
						null
				);
		hitsAssert.extracting( Triplet::elem1 )
				.usingElementComparator( APPROX_M_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistanceFromCenter2( 1 ),
						dataSet.getFieldDistanceFromCenter2( 2 ),
						dataSet.getFieldDistanceFromCenter2( 3 ),
						null
				);
		hitsAssert.extracting( Triplet::elem2 )
				.usingElementComparator( APPROX_KM_COMPARATOR )
				.containsExactlyInAnyOrder(
						dataSet.getFieldDistanceFromCenter1( 1 ) / 1000,
						dataSet.getFieldDistanceFromCenter1( 2 ) / 1000,
						dataSet.getFieldDistanceFromCenter1( 3 ) / 1000,
						null
				);
	}

	private String getFieldPath() {
		return mainIndex.binding().getFieldPath( fieldStructure, fieldType );
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
			if ( fieldStructure.isSingleValued() ) {
				mainIndexer.add( documentProvider( emptyDocId( 1 ), routingKey,
						document -> mainIndex.binding().initSingleValued( fieldType, fieldStructure.location,
								document, null ) ) );
				mainIndexer.add( documentProvider( docId( 1 ), routingKey,
						document -> mainIndex.binding().initSingleValued( fieldType, fieldStructure.location,
								document, getFieldValue( 1 ) ) ) );
				mainIndexer.add( documentProvider( docId( 2 ), routingKey,
						document -> mainIndex.binding().initSingleValued( fieldType, fieldStructure.location,
								document, getFieldValue( 2 ) ) ) );
				mainIndexer.add( documentProvider( docId( 3 ), routingKey,
						document -> mainIndex.binding().initSingleValued( fieldType, fieldStructure.location,
								document, getFieldValue( 3 ) ) ) );

				sortableIndexer.add( documentProvider( docId( 2 ), routingKey,
						document -> sortableIndex.binding().initSingleValued( fieldType, fieldStructure.location,
								document, getSortableFieldValue( 2 ) ) ) );
				sortableIndexer.add( documentProvider( docId( 1 ), routingKey,
						document -> sortableIndex.binding().initSingleValued( fieldType, fieldStructure.location,
								document, getSortableFieldValue( 1 ) ) ) );
				sortableIndexer.add( documentProvider( docId( 3 ), routingKey,
						document -> sortableIndex.binding().initSingleValued( fieldType, fieldStructure.location,
								document, getSortableFieldValue( 3 ) ) ) );
			}
			else {
				// TODO HSEARCH-3391 support multi-valued projections
			}
		}

		private GeoPoint getFieldValue(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getSingle().get( documentNumber - 1 );
		}

		private GeoPoint getSortableFieldValue(int documentNumber) {
			return AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingle().get( documentNumber - 1 );
		}

		private double getFieldDistanceFromCenter1(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getSingleDistancesFromCenterPoint1()
					.get( documentNumber - 1 );
		}

		private double getFieldDistanceFromCenter2(int documentNumber) {
			return IndexableGeoPointWithDistanceFromCenterValues.INSTANCE.getSingleDistancesFromCenterPoint2()
					.get( documentNumber - 1 );
		}

		private Double getSortableFieldDistanceFromCenter(int documentNumber) {
			return AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingleDistancesFromCenterPoint()
					.get( documentNumber - 1 );
		}
	}
}
