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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
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

	@Parameterized.Parameters(name = "{0} - {1}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			parameters.add( new Object[] { fieldStructure, null } );
			for ( SortMode sortMode : SortMode.values() ) {
				parameters.add( new Object[] { fieldStructure, sortMode } );
			}
		}
		return parameters.toArray( new Object[0][] );
	}

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

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	private final TestedFieldStructure fieldStructure;
	private final SortMode sortMode;

	public DistanceSearchSortBaseIT(TestedFieldStructure fieldStructure, SortMode sortMode) {
		this.fieldStructure = fieldStructure;
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
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery(
				b -> b.distance( fieldPathForAscendingOrderTests, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() )
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery(
				b -> b.distance( fieldPathForAscendingOrderTests, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() )
						.asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery(
				b -> b.distance( fieldPathForDescendingOrderTests, CENTER_POINT ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), EMPTY_ID, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery(
				b -> b.distance( fieldPathForDescendingOrderTests, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() )
						.desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), EMPTY_ID, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
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
		return SortMode.MEDIAN.equals( sortMode ) && fieldStructure.isInNested();
	}

	private boolean isSum() {
		return SortMode.SUM.equals( sortMode );
	}

	private SearchQuery<DocumentReference> simpleQuery(
			Function<? super SearchSortFactory, ? extends DistanceSortOptionsStep<?, ?>> sortContributor) {
		StubMappingScope scope = index.createScope();
		return scope.query()
				.where( f -> f.matchAll() )
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
					.field( getFieldPath( parent -> "discriminator" ) )
					.matching( "included" ) );
		}
		else {
			return optionsStep;
		}
	}

	private String getFieldPath(SortOrder expectedOrder) {
		return getFieldPath( parentMapping -> getRelativeFieldName( expectedOrder ) );
	}

	private String getFieldPath(Function<AbstractObjectMapping, String> relativeFieldNameFunction) {
		switch ( fieldStructure.location ) {
			case ROOT:
				return relativeFieldNameFunction.apply( index.binding() );
			case IN_FLATTENED:
				return index.binding().flattenedObject.relativeFieldName
						+ "." + relativeFieldNameFunction.apply( index.binding().flattenedObject );
			case IN_NESTED:
				return index.binding().nestedObject.relativeFieldName
						+ "." + relativeFieldNameFunction.apply( index.binding().nestedObject );
			case IN_NESTED_TWICE:
				return index.binding().nestedObject.relativeFieldName
						+ "." + index.binding().nestedObject.nestedObject.relativeFieldName
						+ "." + relativeFieldNameFunction.apply( index.binding().nestedObject.nestedObject );
			default:
				throw new IllegalStateException( "Unexpected value: " + fieldStructure.location );
		}
	}

	private String getRelativeFieldName(SortOrder expectedOrder) {
		if ( fieldStructure.isSingleValued() ) {
			return "geoPoint";
		}
		else {
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
		}
	}

	private static void initDocument(DocumentElement document, Integer ordinal) {
		initAllFields( index.binding(), document, ordinal );

		DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
		initAllFields( index.binding().flattenedObject, flattenedObject, ordinal );

		// The nested object is split into four objects:
		// the first two are included by the filter and each hold part of the values that will be sorted on,
		// and the last two are excluded by the filter and hold garbage values that, if they were taken into account,
		// would mess with the sort order and eventually fail at least *some* tests.

		DocumentElement nestedObject0 = document.addObject( index.binding().nestedObject.self );
		nestedObject0.addValue( index.binding().nestedObject.discriminator, "included" );
		initAllFields( index.binding().nestedObject, nestedObject0, ordinal, ValueSelection.FIRST_PARTITION );

		DocumentElement nestedObject1 = document.addObject( index.binding().nestedObject.self );
		nestedObject1.addValue( index.binding().nestedObject.discriminator, "included" );
		initAllFields( index.binding().nestedObject, nestedObject1, ordinal, ValueSelection.SECOND_PARTITION );

		DocumentElement nestedObject2 = document.addObject( index.binding().nestedObject.self );
		nestedObject2.addValue( index.binding().nestedObject.discriminator, "excluded" );
		initAllFields( index.binding().nestedObject, nestedObject2, ordinal == null ? null : ordinal - 1 );

		DocumentElement nestedObject3 = document.addObject( index.binding().nestedObject.self );
		nestedObject3.addValue( index.binding().nestedObject.discriminator, "excluded" );
		initAllFields( index.binding().nestedObject, nestedObject3, ordinal == null ? null : ordinal + 1 );

		// Same for the second level of nesting

		DocumentElement nestedNestedObject0 = nestedObject0.addObject( index.binding().nestedObject.nestedObject.self );
		nestedNestedObject0.addValue( index.binding().nestedObject.nestedObject.discriminator, "included" );
		initAllFields( index.binding().nestedObject.nestedObject, nestedNestedObject0, ordinal, ValueSelection.FIRST_PARTITION );

		DocumentElement nestedNestedObject1 = nestedObject0.addObject( index.binding().nestedObject.nestedObject.self );
		nestedNestedObject1.addValue( index.binding().nestedObject.nestedObject.discriminator, "included" );
		initAllFields( index.binding().nestedObject.nestedObject, nestedNestedObject1, ordinal, ValueSelection.SECOND_PARTITION );

		DocumentElement nestedNestedObject2 = nestedObject0.addObject( index.binding().nestedObject.nestedObject.self );
		nestedNestedObject2.addValue( index.binding().nestedObject.nestedObject.discriminator, "excluded" );
		initAllFields( index.binding().nestedObject.nestedObject, nestedNestedObject2, ordinal == null ? null : ordinal - 1 );

		DocumentElement nestedNestedObject3 = nestedObject0.addObject( index.binding().nestedObject.nestedObject.self );
		nestedNestedObject3.addValue( index.binding().nestedObject.nestedObject.discriminator, "excluded" );
		initAllFields( index.binding().nestedObject.nestedObject, nestedNestedObject3, ordinal == null ? null : ordinal + 1 );
	}

	private static void initAllFields(AbstractObjectMapping mapping, DocumentElement document, Integer ordinal) {
		initAllFields( mapping, document, ordinal, ValueSelection.ALL );
	}

	private static void initAllFields(AbstractObjectMapping mapping, DocumentElement document, Integer ordinal,
			ValueSelection valueSelection) {
		if ( EnumSet.of( ValueSelection.ALL, ValueSelection.FIRST_PARTITION ).contains( valueSelection ) ) {
			addSingleValue( mapping.geoPoint, document, ordinal );
		}

		Integer startIndexForMultiValued;
		Integer endIndexForMultiValued;

		switch ( valueSelection ) {
			case FIRST_PARTITION:
				startIndexForMultiValued = 0;
				endIndexForMultiValued = 1;
				break;
			case SECOND_PARTITION:
				startIndexForMultiValued = 1;
				endIndexForMultiValued = null;
				break;
			case ALL:
			default:
				startIndexForMultiValued = null;
				endIndexForMultiValued = null;
				break;
		}

		addMultipleValues( mapping.geoPointAscendingSum, document, SortMode.SUM, ordinal,
				startIndexForMultiValued, endIndexForMultiValued );
		addMultipleValues( mapping.geoPointAscendingMin, document, SortMode.MIN, ordinal,
				startIndexForMultiValued, endIndexForMultiValued );
		addMultipleValues( mapping.geoPointAscendingMax, document, SortMode.MAX, ordinal,
				startIndexForMultiValued, endIndexForMultiValued );
		addMultipleValues( mapping.geoPointAscendingAvg, document, SortMode.AVG, ordinal,
				startIndexForMultiValued, endIndexForMultiValued );
		addMultipleValues( mapping.geoPointAscendingMedian, document, SortMode.MEDIAN, ordinal,
				startIndexForMultiValued, endIndexForMultiValued );
	}

	private static void addSingleValue(IndexFieldReference<GeoPoint> reference, DocumentElement document, Integer ordinal) {
		if ( ordinal != null ) {
			document.addValue( reference, AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingle().get( ordinal ) );
		}
	}

	private static void addMultipleValues(IndexFieldReference<GeoPoint> reference, DocumentElement documentElement,
			SortMode sortMode, Integer ordinal,
			Integer startIndex, Integer endIndex) {
		if ( ordinal == null ) {
			return;
		}
		List<GeoPoint> values = AscendingUniqueDistanceFromCenterValues.INSTANCE.getMultiResultingInSingle( sortMode )
				.get( ordinal );
		if ( values.isEmpty() ) {
			return;
		}
		if ( startIndex == null ) {
			startIndex = 0;
		}
		if ( endIndex == null ) {
			endIndex = values.size();
		}
		for ( int i = startIndex; i < endIndex; i++ ) {
			documentElement.addValue( reference, values.get( i ) );
		}
	}

	private static void initData() {
		IndexIndexingPlan<?> plan = index.createIndexingPlan();
		// Important: do not index the documents in the expected order after sorts
		plan.add( referenceProvider( DOCUMENT_3 ), document -> initDocument( document, DOCUMENT_3_ORDINAL ) );
		plan.add( referenceProvider( DOCUMENT_1 ), document -> initDocument( document, DOCUMENT_1_ORDINAL ) );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> initDocument( document, DOCUMENT_2_ORDINAL ) );
		plan.add( referenceProvider( EMPTY_ID ), document -> initDocument( document, null ) );

		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );
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

	private static class IndexBinding extends AbstractObjectMapping {
		final FirstLevelObjectMapping flattenedObject;
		final FirstLevelObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			super( root );
			flattenedObject = FirstLevelObjectMapping.create( root, "flattenedObject",
					ObjectFieldStorage.FLATTENED, false );
			nestedObject = FirstLevelObjectMapping.create( root, "nestedObject",
					ObjectFieldStorage.NESTED, true );
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;

		final SecondLevelObjectMapping nestedObject;

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage, boolean multiValued) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			if ( multiValued ) {
				objectField.multiValued();
			}
			return new FirstLevelObjectMapping( relativeFieldName, objectField );
		}

		private FirstLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();

			discriminator = objectField.field( "discriminator", f -> f.asString() ).toReference();

			nestedObject = SecondLevelObjectMapping.create( objectField, "nestedObject",
					ObjectFieldStorage.NESTED );
		}
	}

	private static class SecondLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;

		public static SecondLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			objectField.multiValued();
			return new SecondLevelObjectMapping( relativeFieldName, objectField );
		}

		private SecondLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();

			discriminator = objectField.field( "discriminator", f -> f.asString() ).toReference();
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
					asList( getSingle().get( 7 ),getSingle().get( 7 ), getSingle().get( 7 ) ) // ~14km
			);
		}
	}

	private enum ValueSelection {
		FIRST_PARTITION,
		SECOND_PARTITION,
		ALL
	}
}
