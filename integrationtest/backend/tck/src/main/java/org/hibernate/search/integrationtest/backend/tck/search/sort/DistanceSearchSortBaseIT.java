/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
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
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

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

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for ( IndexFieldStructure indexFieldStructure : IndexFieldStructure.values() ) {
			parameters.add( new Object[] { indexFieldStructure } );
		}
		return parameters.toArray( new Object[0][] );
	}

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY_ID = "empty";

	private static final GeoPoint CENTER_POINT = GeoPoint.of( 45.757864, 4.834496 );
	private static final GeoPoint DISTANCE_1_POINT = GeoPoint.of( 45.7541719, 4.8386221 );
	private static final GeoPoint DISTANCE_2_POINT = GeoPoint.of( 45.7530374, 4.8510299 );
	private static final GeoPoint DISTANCE_3_POINT = GeoPoint.of( 45.7705687, 4.835233 );

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

	public DistanceSearchSortBaseIT(IndexFieldStructure indexFieldStructure) {
		this.indexFieldStructure = indexFieldStructure;
	}

	@Test
	public void simple() {
		String fieldPath = getFieldPath();

		SearchQuery<DocumentReference> query = simpleQuery(
				b -> b.distance( fieldPath, CENTER_POINT )
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery( b -> b.distance( fieldPath, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery( b -> b.distance( fieldPath, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );

		query = simpleQuery(
				b -> b.distance( fieldPath, CENTER_POINT ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( b -> b.distance( fieldPath, CENTER_POINT.getLatitude(), CENTER_POINT.getLongitude() ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	private SearchQuery<DocumentReference> simpleQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private String getFieldPath() {
		switch ( indexFieldStructure ) {
			case ROOT:
				return "geoPoint";
			case IN_FLATTENED:
				return "flattenedObject.geoPoint";
			case IN_NESTED:
				return "nestedObject.geoPoint";
			case IN_NESTED_TWICE:
				return "nestedObject.nestedObject.geoPoint";
			default:
				throw new IllegalStateException( "Unexpected value: " + indexFieldStructure );
		}
	}

	private static void initDocument(DocumentElement document, GeoPoint geoPoint) {
		addValue( indexMapping.geoPoint, document, geoPoint );

		// Note: this object must be single-valued for these tests
		DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
		addValue( indexMapping.flattenedObject.geoPoint, flattenedObject, geoPoint );

		// Note: this object must be single-valued for these tests
		DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
		addValue( indexMapping.nestedObject.geoPoint, nestedObject, geoPoint );

		// Note: this object must be single-valued for these tests
		DocumentElement nestedObjectInNestedObject =
				nestedObject.addObject( indexMapping.nestedObject.nestedObject.self );
		addValue( indexMapping.nestedObject.nestedObject.geoPoint, nestedObjectInNestedObject, geoPoint );
	}

	private static void addValue(IndexFieldReference<GeoPoint> reference, DocumentElement document, GeoPoint geoPoint) {
		if ( geoPoint != null ) {
			document.addValue( reference, geoPoint );
		}
	}

	private static void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		// Important: do not index the documents in the expected order after sorts
		plan.add( referenceProvider( DOCUMENT_3 ), document -> initDocument( document, DISTANCE_3_POINT ) );
		plan.add( referenceProvider( DOCUMENT_1 ), document -> initDocument( document, DISTANCE_1_POINT ) );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> initDocument( document, DISTANCE_2_POINT ) );
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

		AbstractObjectMapping(IndexSchemaElement self) {
			geoPoint = self.field( "geoPoint", f -> f.asGeoPoint().sortable( Sortable.YES ) )
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

}
