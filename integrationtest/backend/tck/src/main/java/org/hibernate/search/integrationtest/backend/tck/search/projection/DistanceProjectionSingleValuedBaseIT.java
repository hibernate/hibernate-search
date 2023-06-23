/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding.NO_ADDITIONAL_CONFIGURATION;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_1;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues.CENTER_POINT_2;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators.APPROX_KM_COMPARATOR;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators.APPROX_MILES_COMPARATOR;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators.APPROX_M_COMPARATOR;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.AbstractObjectBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableGeoPointWithDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
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
 * Tests basic behavior of projections on the distance between a given point and the value of a single-valued field.
 */
@RunWith(Parameterized.class)
public class DistanceProjectionSingleValuedBaseIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;
	private static final Set<FieldTypeDescriptor<GeoPoint>> supportedFieldTypes = Collections.singleton( fieldType );
	private static List<DataSet> dataSets;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			if ( fieldStructure.isMultiValued() ) {
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
			SimpleMappedIndex.of(
					root -> SingleFieldIndexBinding.createWithSingleValuedNestedFields(
							root,
							supportedFieldTypes,
							TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
									? NO_ADDITIONAL_CONFIGURATION
									: c -> c.projectable( Projectable.YES )
					)
			)
					.name( "main" );
	private static final SimpleMappedIndex<SingleFieldIndexBinding> sortableIndex =
			SimpleMappedIndex.of(
					root -> SingleFieldIndexBinding.createWithSingleValuedNestedFields(
							root,
							supportedFieldTypes,
							TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
									? NO_ADDITIONAL_CONFIGURATION
									: c -> c.projectable( Projectable.YES ).sortable( Sortable.YES )
					)
			)
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

	public DistanceProjectionSingleValuedBaseIT(TestedFieldStructure fieldStructure, DataSet dataSet) {
		this.fieldStructure = fieldStructure;
		this.dataSet = dataSet;
	}

	@Test
	public void simple() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		assertThatQuery( scope.query()
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
	 * Test requesting a multi-valued projection on a single-valued field.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3391")
	public void multi() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		assertThatQuery( scope.query()
				.select( f -> f.distance( fieldPath, CENTER_POINT_1 ).multi() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hits().asIs()
				.usingElementComparator( TestComparators.lexicographic( APPROX_M_COMPARATOR ) )
				.containsExactlyInAnyOrder(
						Collections.singletonList( dataSet.getFieldDistanceFromCenter1( 1 ) ),
						Collections.singletonList( dataSet.getFieldDistanceFromCenter1( 2 ) ),
						Collections.singletonList( dataSet.getFieldDistanceFromCenter1( 3 ) ),
						// Empty document
						TckConfiguration.get().getBackendFeatures().projectionPreservesNulls()
								? Collections.singletonList( null )
								: Collections.emptyList()
				);
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@Test
	public void duplicated() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		ListAssert<Pair<Double, Double>> hitsAssert = assertThatQuery( scope.query()
				.select( f -> f.composite()
						.from( f.distance( fieldPath, CENTER_POINT_1 ),
								f.distance( fieldPath, CENTER_POINT_1 ) )
						.as( Pair::new ) )
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

		assertThatQuery( scope.query()
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

		assertThatQuery( scope.query()
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

		assertThatQuery( scope.query()
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

		assertThatQuery( scope.query()
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

		ListAssert<Triplet<Double, Double, Double>> hitsAssert = assertThatQuery( scope.query()
				.select( f -> f.composite()
						.from( f.distance( getFieldPath(), CENTER_POINT_1 ),
								f.distance( getFieldPath(), CENTER_POINT_2 ),
								f.distance( getFieldPath(), CENTER_POINT_1 ).unit( DistanceUnit.KILOMETERS ) )
						.as( Triplet::new ) )
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void factoryWithRoot() {
		AbstractObjectBinding parentObjectBinding = mainIndex.binding().getParentObject( fieldStructure );

		assumeTrue( "This test is only relevant when the field is located on an object field",
				parentObjectBinding.absolutePath != null );

		assertThatQuery( mainIndex.query()
				.select( f -> f.withRoot( parentObjectBinding.absolutePath )
						.distance( parentObjectBinding.getRelativeFieldName( fieldStructure, fieldType ), CENTER_POINT_1 ) )
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
