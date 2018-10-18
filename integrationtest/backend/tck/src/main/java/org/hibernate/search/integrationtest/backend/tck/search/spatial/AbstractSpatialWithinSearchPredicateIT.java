/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;

import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractSpatialWithinSearchPredicateIT {

	protected static final String INDEX_NAME = "IndexName";

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

	protected IndexAccessors indexAccessors;
	protected StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	protected void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( OURSON_QUI_BOIT_ID ), document -> {
			indexAccessors.string.write( document, OURSON_QUI_BOIT_STRING );
			indexAccessors.geoPoint.write( document, OURSON_QUI_BOIT_GEO_POINT );
			indexAccessors.geoPoint_1.write( document, GeoPoint.of( OURSON_QUI_BOIT_GEO_POINT.getLatitude() - 1,
					OURSON_QUI_BOIT_GEO_POINT.getLongitude() - 1 ) );
			indexAccessors.geoPoint_2.write( document, GeoPoint.of( OURSON_QUI_BOIT_GEO_POINT.getLatitude() - 2,
					OURSON_QUI_BOIT_GEO_POINT.getLongitude() - 2 ) );
			indexAccessors.geoPoint_with_longName.write( document, OURSON_QUI_BOIT_GEO_POINT );
		} );
		workPlan.add( referenceProvider( IMOUTO_ID ), document -> {
			indexAccessors.string.write( document, IMOUTO_STRING );
			indexAccessors.geoPoint.write( document, IMOUTO_GEO_POINT );
			indexAccessors.geoPoint_1.write( document, GeoPoint.of( IMOUTO_GEO_POINT.getLatitude() - 1,
					IMOUTO_GEO_POINT.getLongitude() - 1 ) );
			indexAccessors.geoPoint_2.write( document, GeoPoint.of( IMOUTO_GEO_POINT.getLatitude() - 2,
					IMOUTO_GEO_POINT.getLongitude() - 2 ) );
			indexAccessors.geoPoint_with_longName.write( document, IMOUTO_GEO_POINT );
		} );
		workPlan.add( referenceProvider( CHEZ_MARGOTTE_ID ), document -> {
			indexAccessors.string.write( document, CHEZ_MARGOTTE_STRING );
			indexAccessors.geoPoint.write( document, CHEZ_MARGOTTE_GEO_POINT );
			indexAccessors.geoPoint_1.write( document, GeoPoint.of( CHEZ_MARGOTTE_GEO_POINT.getLatitude() - 1,
					CHEZ_MARGOTTE_GEO_POINT.getLongitude() - 1 ) );
			indexAccessors.geoPoint_2.write( document, GeoPoint.of( CHEZ_MARGOTTE_GEO_POINT.getLatitude() - 2,
					CHEZ_MARGOTTE_GEO_POINT.getLongitude() - 2 ) );
			indexAccessors.geoPoint_with_longName.write( document, CHEZ_MARGOTTE_GEO_POINT );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, OURSON_QUI_BOIT_ID, IMOUTO_ID, CHEZ_MARGOTTE_ID, EMPTY_ID );
	}

	protected static class IndexAccessors {
		final IndexFieldAccessor<GeoPoint> geoPoint;
		final IndexFieldAccessor<GeoPoint> geoPoint_1;
		final IndexFieldAccessor<GeoPoint> geoPoint_2;
		final IndexFieldAccessor<GeoPoint> geoPoint_with_longName;
		final IndexFieldAccessor<GeoPoint> nonProjectableGeoPoint;
		final IndexFieldAccessor<GeoPoint> unsortableGeoPoint;
		final IndexFieldAccessor<String> string;

		IndexAccessors(IndexSchemaElement root) {
			geoPoint = root.field( "geoPoint" ).asGeoPoint().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
			geoPoint_1 = root.field( "geoPoint_1" ).asGeoPoint().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
			geoPoint_2 = root.field( "geoPoint_2" ).asGeoPoint().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
			geoPoint_with_longName = root.field( "geoPoint_with_a_veeeeeeeeeeeeeeeeeeeeerrrrrrrrrrrrrrrrrryyyyyyyyyyyyyyyy_long_name" )
					.asGeoPoint().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
			nonProjectableGeoPoint = root.field( "nonProjectableGeoPoint" ).asGeoPoint().projectable( Projectable.NO ).createAccessor();
			unsortableGeoPoint = root.field( "unsortableGeoPoint" ).asGeoPoint().sortable( Sortable.NO ).createAccessor();
			string = root.field( "string" ).asString().projectable( Projectable.YES ).sortable( Sortable.YES ).createAccessor();
		}
	}
}
