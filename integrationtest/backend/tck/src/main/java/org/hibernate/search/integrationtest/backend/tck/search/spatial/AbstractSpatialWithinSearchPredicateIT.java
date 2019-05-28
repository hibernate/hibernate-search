/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;

import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractSpatialWithinSearchPredicateIT {

	protected static final String INDEX_NAME = "IndexName";
	protected static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	protected static final String UNSEARCHABLE_FIELDS_INDEX_NAME = "IndexWithUnsearchableFields";

	protected static final String OURSON_QUI_BOIT_ID = "ourson qui boit";
	protected static final GeoPoint OURSON_QUI_BOIT_GEO_POINT = GeoPoint.of( 45.7705687,4.835233 );
	protected static final String OURSON_QUI_BOIT_STRING = "L'ourson qui boit";

	protected static final String IMOUTO_ID = "imouto";
	protected static final GeoPoint IMOUTO_GEO_POINT = GeoPoint.of( 45.7541719, 4.8386221 );
	protected static final String IMOUTO_STRING = "Imouto";

	protected static final String CHEZ_MARGOTTE_ID = "chez margotte";
	protected static final GeoPoint CHEZ_MARGOTTE_GEO_POINT = GeoPoint.of( 45.7530374, 4.8510299 );
	protected static final String CHEZ_MARGOTTE_STRING = "Chez Margotte";

	protected static final String EMPTY_ID = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	protected IndexMapping indexMapping;
	protected StubMappingIndexManager indexManager;

	protected StubMappingIndexManager compatibleIndexManager;

	// TODO HSEARCH-3593 test other incompatibilities ( projection, sort and so on and so forth )
	protected StubMappingIndexManager unsearchableFieldsIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						UNSEARCHABLE_FIELDS_INDEX_NAME,
						ctx -> new UnsearchableFieldsIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.unsearchableFieldsIndexManager = indexManager
				)
				.setup();

		initData();
	}

	protected void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( OURSON_QUI_BOIT_ID ), document -> {
			document.addValue( indexMapping.string, OURSON_QUI_BOIT_STRING );
			document.addValue( indexMapping.geoPoint, OURSON_QUI_BOIT_GEO_POINT );
			document.addValue( indexMapping.geoPoint_1, GeoPoint.of( OURSON_QUI_BOIT_GEO_POINT.getLatitude() - 1,
					OURSON_QUI_BOIT_GEO_POINT.getLongitude() - 1 ) );
			document.addValue( indexMapping.geoPoint_2, GeoPoint.of( OURSON_QUI_BOIT_GEO_POINT.getLatitude() - 2,
					OURSON_QUI_BOIT_GEO_POINT.getLongitude() - 2 ) );
			document.addValue( indexMapping.geoPoint_with_longName, OURSON_QUI_BOIT_GEO_POINT );
		} );
		workPlan.add( referenceProvider( IMOUTO_ID ), document -> {
			document.addValue( indexMapping.string, IMOUTO_STRING );
			document.addValue( indexMapping.geoPoint, IMOUTO_GEO_POINT );
			document.addValue( indexMapping.geoPoint_1, GeoPoint.of( IMOUTO_GEO_POINT.getLatitude() - 1,
					IMOUTO_GEO_POINT.getLongitude() - 1 ) );
			document.addValue( indexMapping.geoPoint_2, GeoPoint.of( IMOUTO_GEO_POINT.getLatitude() - 2,
					IMOUTO_GEO_POINT.getLongitude() - 2 ) );
			document.addValue( indexMapping.geoPoint_with_longName, IMOUTO_GEO_POINT );
		} );
		workPlan.add( referenceProvider( CHEZ_MARGOTTE_ID ), document -> {
			document.addValue( indexMapping.string, CHEZ_MARGOTTE_STRING );
			document.addValue( indexMapping.geoPoint, CHEZ_MARGOTTE_GEO_POINT );
			document.addValue( indexMapping.geoPoint_1, GeoPoint.of( CHEZ_MARGOTTE_GEO_POINT.getLatitude() - 1,
					CHEZ_MARGOTTE_GEO_POINT.getLongitude() - 1 ) );
			document.addValue( indexMapping.geoPoint_2, GeoPoint.of( CHEZ_MARGOTTE_GEO_POINT.getLatitude() - 2,
					CHEZ_MARGOTTE_GEO_POINT.getLongitude() - 2 ) );
			document.addValue( indexMapping.geoPoint_with_longName, CHEZ_MARGOTTE_GEO_POINT );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID, CHEZ_MARGOTTE_ID, EMPTY_ID );
	}

	protected static class IndexMapping {
		final IndexFieldReference<GeoPoint> geoPoint;
		final IndexFieldReference<GeoPoint> geoPoint_1;
		final IndexFieldReference<GeoPoint> geoPoint_2;
		final IndexFieldReference<GeoPoint> geoPoint_with_longName;
		final IndexFieldReference<GeoPoint> nonProjectableGeoPoint;
		final IndexFieldReference<GeoPoint> unsortableGeoPoint;
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			geoPoint = root.field(
					"geoPoint", f -> f.asGeoPoint().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
			geoPoint_1 = root.field(
					"geoPoint_1", f -> f.asGeoPoint().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
			geoPoint_2 = root.field(
					"geoPoint_2", f -> f.asGeoPoint().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
			geoPoint_with_longName = root.field(
					"geoPoint_with_a_veeeeeeeeeeeeeeeeeeeeerrrrrrrrrrrrrrrrrryyyyyyyyyyyyyyyy_long_name",
					f -> f.asGeoPoint().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
			nonProjectableGeoPoint = root.field(
					"nonProjectableGeoPoint",
					f -> f.asGeoPoint().projectable( Projectable.NO )
			)
					.toReference();
			unsortableGeoPoint = root.field(
					"unsortableGeoPoint",
					f -> f.asGeoPoint().sortable( Sortable.NO )
			)
					.toReference();
			string = root.field(
					"string",
					f -> f.asString().projectable( Projectable.YES ).sortable( Sortable.YES )
			)
					.toReference();
		}
	}

	protected static class UnsearchableFieldsIndexMapping {
		final IndexFieldReference<GeoPoint> geoPoint;

		UnsearchableFieldsIndexMapping(IndexSchemaElement root) {
			geoPoint = root.field(
					// make the field not searchable
					"geoPoint", f -> f.asGeoPoint().searchable( Searchable.NO )
			)
					.toReference();
		}
	}
}
