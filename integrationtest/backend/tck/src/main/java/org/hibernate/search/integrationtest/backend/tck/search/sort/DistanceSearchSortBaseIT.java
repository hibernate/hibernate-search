/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static java.util.Arrays.asList;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldValueCardinality;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests basic behavior of sorts by distance.
 */
@RunWith(Parameterized.class)
public class DistanceSearchSortBaseIT {

	@Parameterized.Parameters(name = "{0} - {1} - {2}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for ( IndexFieldStructure indexFieldStructure : IndexFieldStructure.values() ) {
			for ( IndexFieldValueCardinality indexFieldValueCardinality : IndexFieldValueCardinality.values() ) {
				parameters.add( new Object[] { indexFieldStructure, indexFieldValueCardinality, null } );
				for ( SortMode sortMode : SortMode.values() ) {
					parameters.add( new Object[] { indexFieldStructure, indexFieldValueCardinality, sortMode } );
				}
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY_ID = "empty";

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

	private static IndexMapping indexMapping;
	private static StubMappingIndexManager indexManager;

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> DistanceSearchSortBaseIT.indexManager = indexManager
				)
				.setup();

		initData();
	}

	private final IndexFieldStructure indexFieldStructure;
	private final IndexFieldValueCardinality indexFieldValueCardinality;
	private final SortMode sortMode;

	public DistanceSearchSortBaseIT(IndexFieldStructure indexFieldStructure,
			IndexFieldValueCardinality indexFieldValueCardinality, SortMode sortMode) {
		this.indexFieldStructure = indexFieldStructure;
		this.indexFieldValueCardinality = indexFieldValueCardinality;
		this.sortMode = sortMode;
	}

	@Test
	public void simple() {
		assumeTestParametersWork();

		String fieldPathForAscendingOrderTests = getFieldPath( SortOrder.ASC );
		String fieldPathForDescendingOrderTests = getFieldPath( SortOrder.DESC );

		SearchQuery<DocumentReference> query = simpleQuery(
				b -> b.distance( fieldPathForAscendingOrderTests, CENTER_POINT )
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery(
				b -> b.distance( fieldPathForAscendingOrderTests, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() )
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery(
				b -> b.distance( fieldPathForAscendingOrderTests, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() )
						.asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery(
				b -> b.distance( fieldPathForDescendingOrderTests, CENTER_POINT ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery(
				b -> b.distance( fieldPathForDescendingOrderTests, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() )
						.desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void medianWithNestedField() {
		Assume.assumeTrue(
				"This test is only relevant when using SortMode.MEDIAN in nested fields",
				isMedianWithNestedField()
		);

		String fieldPath = getFieldPath( SortOrder.ASC );

		Assertions.assertThatThrownBy( () -> simpleQuery( b -> b.distance( fieldPath, CENTER_POINT ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot compute the median across nested documents",
						fieldPath
				);
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3103" })
	public void sumWithTemporalField() {
		Assume.assumeTrue(
				"This test is only relevant when using SortMode.SUM",
				isSum()
		);

		String fieldPath = getFieldPath( SortOrder.ASC );

		Assertions.assertThatThrownBy( () -> simpleQuery( b -> b.distance( fieldPath, CENTER_POINT ) ) )
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
		return SortMode.MEDIAN.equals( sortMode )
				&& EnumSet.of( IndexFieldStructure.IN_NESTED, IndexFieldStructure.IN_NESTED_TWICE )
				.contains( indexFieldStructure );
	}

	private boolean isSum() {
		return SortMode.SUM.equals( sortMode );
	}

	private SearchQuery<DocumentReference> simpleQuery(
			Function<? super SearchSortFactory, ? extends DistanceSortOptionsStep<?, ?>> sortContributor) {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor.andThen( o -> {
					if ( sortMode != null ) {
						return o.mode( sortMode );
					}
					else {
						return o;
					}
				} ) )
				.toQuery();
	}

	private String getFieldPath(SortOrder expectedOrder) {
		switch ( indexFieldStructure ) {
			case ROOT:
				return getRelativeFieldName( expectedOrder );
			case IN_FLATTENED:
				return "flattenedObject." + getRelativeFieldName( expectedOrder );
			case IN_NESTED:
				return "nestedObject." + getRelativeFieldName( expectedOrder );
			case IN_NESTED_TWICE:
				return "nestedObject.nestedObject." + getRelativeFieldName( expectedOrder );
			default:
				throw new IllegalStateException( "Unexpected value: " + indexFieldStructure );
		}
	}

	private String getRelativeFieldName(SortOrder expectedOrder) {
		switch ( indexFieldValueCardinality ) {
			case SINGLE_VALUED:
				// Sort on a single-valued field.
				return "geoPoint";
			case MULTI_VALUED:
				if ( sortMode == null ) {
					// Default sort mode: min in ascending order, max in descending order
					switch ( expectedOrder ) {
						case ASC:
							return "geoPoint_ascendingMin";
						case DESC:
							return "geoPoint_ascendingMax";
						default:
							throw new IllegalStateException( "Unexpected sort order: " + expectedOrder );
					}
				}
				else {
					switch ( sortMode ) {
						case SUM:
							return "geoPoint_ascendingSum";
						case MIN:
							return "geoPoint_ascendingMin";
						case MAX:
							return "geoPoint_ascendingMax";
						case AVG:
							return "geoPoint_ascendingAvg";
						case MEDIAN:
							return "geoPoint_ascendingMedian";
						default:
							throw new IllegalStateException( "Unexpected sort mode: " + sortMode );
					}
				}
			default:
				throw new IllegalStateException( "Unexpected field value cardinality: " + indexFieldValueCardinality );
		}
	}

	private static void initDocument(DocumentElement document, Integer ordinal) {
		initAllFields( indexMapping, document, ordinal );

		DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
		initAllFields( indexMapping.flattenedObject, flattenedObject, ordinal );

		DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
		initAllFields( indexMapping.nestedObject, nestedObject, ordinal );

		DocumentElement nestedObjectInNestedObject =
				nestedObject.addObject( indexMapping.nestedObject.nestedObject.self );
		initAllFields( indexMapping.nestedObject.nestedObject, nestedObjectInNestedObject, ordinal );
	}

	private static void initAllFields(AbstractObjectMapping mapping, DocumentElement document, Integer ordinal) {
		addSingleValue( mapping.geoPoint, document, ordinal );
		addMultipleValues( mapping.geoPointAscendingSum, document, SortMode.SUM, ordinal );
		addMultipleValues( mapping.geoPointAscendingMin, document, SortMode.MIN, ordinal );
		addMultipleValues( mapping.geoPointAscendingMax, document, SortMode.MAX, ordinal );
		addMultipleValues( mapping.geoPointAscendingAvg, document, SortMode.AVG, ordinal );
		addMultipleValues( mapping.geoPointAscendingMedian, document, SortMode.MEDIAN, ordinal );
	}

	private static void addSingleValue(IndexFieldReference<GeoPoint> reference, DocumentElement document, Integer ordinal) {
		if ( ordinal != null ) {
			document.addValue( reference, AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingle().get( ordinal ) );
		}
	}

	private static <F> void addMultipleValues(IndexFieldReference<GeoPoint> reference, DocumentElement documentElement,
			SortMode sortMode, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		AscendingUniqueDistanceFromCenterValues.INSTANCE.getMultiResultingInSingle( sortMode ).get( ordinal )
				.forEach( value -> documentElement.addValue( reference, value ) );
	}

	private static void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		// Important: do not index the documents in the expected order after sorts
		plan.add( referenceProvider( DOCUMENT_3 ), document -> initDocument( document, DOCUMENT_3_ORDINAL ) );
		plan.add( referenceProvider( DOCUMENT_1 ), document -> initDocument( document, DOCUMENT_1_ORDINAL ) );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> initDocument( document, DOCUMENT_2_ORDINAL ) );
		plan.add( referenceProvider( EMPTY_ID ), document -> initDocument( document, null ) );

		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );
	}

	private static class AbstractObjectMapping {
		final IndexFieldReference<GeoPoint> geoPoint;
		final IndexFieldReference<GeoPoint> geoPointAscendingSum;
		final IndexFieldReference<GeoPoint> geoPointAscendingMin;
		final IndexFieldReference<GeoPoint> geoPointAscendingMax;
		final IndexFieldReference<GeoPoint> geoPointAscendingAvg;
		final IndexFieldReference<GeoPoint> geoPointAscendingMedian;

		AbstractObjectMapping(IndexSchemaElement self) {
			geoPoint = self.field( "geoPoint", f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.toReference();
			geoPointAscendingSum = self.field( "geoPoint_ascendingSum",
					f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.multiValued()
					.toReference();
			geoPointAscendingMin = self.field( "geoPoint_ascendingMin",
					f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.multiValued()
					.toReference();
			geoPointAscendingMax = self.field( "geoPoint_ascendingMax",
					f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.multiValued()
					.toReference();
			geoPointAscendingAvg = self.field( "geoPoint_ascendingAvg",
					f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.multiValued()
					.toReference();
			geoPointAscendingMedian = self.field( "geoPoint_ascendingMedian",
					f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.multiValued()
					.toReference();
		}
	}

	private static class IndexMapping extends AbstractObjectMapping {
		final FirstLevelObjectMapping flattenedObject;
		final FirstLevelObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			super( root );
			flattenedObject = FirstLevelObjectMapping.create( root, "flattenedObject",
					ObjectFieldStorage.FLATTENED );
			nestedObject = FirstLevelObjectMapping.create( root, "nestedObject",
					ObjectFieldStorage.NESTED );
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final IndexObjectFieldReference self;

		final SecondLevelObjectMapping nestedObject;

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			return new FirstLevelObjectMapping( objectField );
		}

		private FirstLevelObjectMapping(IndexSchemaObjectField objectField) {
			super( objectField );
			self = objectField.toReference();

			nestedObject = SecondLevelObjectMapping.create( objectField, "nestedObject",
					ObjectFieldStorage.NESTED );
		}
	}

	private static class SecondLevelObjectMapping extends AbstractObjectMapping {
		final IndexObjectFieldReference self;

		public static SecondLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			return new SecondLevelObjectMapping( objectField );
		}

		private SecondLevelObjectMapping(IndexSchemaObjectField objectField) {
			super( objectField );
			self = objectField.toReference();
		}
	}

	private static class AscendingUniqueDistanceFromCenterValues extends AscendingUniqueTermValues<GeoPoint> {
		private static AscendingUniqueDistanceFromCenterValues INSTANCE = new AscendingUniqueDistanceFromCenterValues();

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
					asList( getSingle().get( 7 ),getSingle().get( 7 ), getSingle().get( 7 ) ) // ~14km
			);
		}
	}

}
