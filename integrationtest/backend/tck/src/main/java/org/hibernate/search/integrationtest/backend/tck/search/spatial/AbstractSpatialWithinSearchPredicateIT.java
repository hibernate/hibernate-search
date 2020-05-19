/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractSpatialWithinSearchPredicateIT {

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
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	protected final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	protected final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	// TODO HSEARCH-3593 test incompatibilities ( projection, sort and so on and so forth )
	protected final SimpleMappedIndex<UnsearchableFieldsIndexBinding> unsearchableFieldsIndex =
			SimpleMappedIndex.of( UnsearchableFieldsIndexBinding::new ).name( "unsearchableFields" );

	@Before
	public void setup() {
		setupHelper.start().withIndexes( mainIndex, compatibleIndex, unsearchableFieldsIndex ).setup();

		initData();
	}

	protected void initData() {
		mainIndex.bulkIndexer()
				.add( OURSON_QUI_BOIT_ID, document -> {
					document.addValue( mainIndex.binding().string, OURSON_QUI_BOIT_STRING );
					document.addValue( mainIndex.binding().geoPoint, OURSON_QUI_BOIT_GEO_POINT );
					document.addValue( mainIndex.binding().geoPoint_1, GeoPoint.of( OURSON_QUI_BOIT_GEO_POINT.latitude() - 1,
							OURSON_QUI_BOIT_GEO_POINT.longitude() - 1 ) );
					document.addValue( mainIndex.binding().geoPoint_2, GeoPoint.of( OURSON_QUI_BOIT_GEO_POINT.latitude() - 2,
							OURSON_QUI_BOIT_GEO_POINT.longitude() - 2 ) );
					document.addValue( mainIndex.binding().geoPoint_with_longName, OURSON_QUI_BOIT_GEO_POINT );
					document.addValue( mainIndex.binding().projectableUnsortableGeoPoint, OURSON_QUI_BOIT_GEO_POINT );
				} )
				.add( IMOUTO_ID, document -> {
					document.addValue( mainIndex.binding().string, IMOUTO_STRING );
					document.addValue( mainIndex.binding().geoPoint, IMOUTO_GEO_POINT );
					document.addValue( mainIndex.binding().geoPoint_1, GeoPoint.of( IMOUTO_GEO_POINT.latitude() - 1,
							IMOUTO_GEO_POINT.longitude() - 1 ) );
					document.addValue( mainIndex.binding().geoPoint_2, GeoPoint.of( IMOUTO_GEO_POINT.latitude() - 2,
							IMOUTO_GEO_POINT.longitude() - 2 ) );
					document.addValue( mainIndex.binding().geoPoint_with_longName, IMOUTO_GEO_POINT );
					document.addValue( mainIndex.binding().projectableUnsortableGeoPoint, IMOUTO_GEO_POINT );
				} )
				.add( CHEZ_MARGOTTE_ID, document -> {
					document.addValue( mainIndex.binding().string, CHEZ_MARGOTTE_STRING );
					document.addValue( mainIndex.binding().geoPoint, CHEZ_MARGOTTE_GEO_POINT );
					document.addValue( mainIndex.binding().geoPoint_1, GeoPoint.of( CHEZ_MARGOTTE_GEO_POINT.latitude() - 1,
							CHEZ_MARGOTTE_GEO_POINT.longitude() - 1 ) );
					document.addValue( mainIndex.binding().geoPoint_2, GeoPoint.of( CHEZ_MARGOTTE_GEO_POINT.latitude() - 2,
							CHEZ_MARGOTTE_GEO_POINT.longitude() - 2 ) );
					document.addValue( mainIndex.binding().geoPoint_with_longName, CHEZ_MARGOTTE_GEO_POINT );
					document.addValue( mainIndex.binding().projectableUnsortableGeoPoint, CHEZ_MARGOTTE_GEO_POINT );
				} )
				.add( EMPTY_ID, document -> { } )
				.join();
	}

	protected static class IndexBinding {
		final IndexFieldReference<GeoPoint> geoPoint;
		final IndexFieldReference<GeoPoint> geoPoint_1;
		final IndexFieldReference<GeoPoint> geoPoint_2;
		final IndexFieldReference<GeoPoint> geoPoint_with_longName;
		final IndexFieldReference<GeoPoint> nonProjectableGeoPoint;
		final IndexFieldReference<GeoPoint> unsortableGeoPoint;
		final IndexFieldReference<GeoPoint> projectableUnsortableGeoPoint;
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
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
			projectableUnsortableGeoPoint = root.field(
					"projectableUnsortableGeoPoint",
					f -> f.asGeoPoint().projectable( Projectable.YES ).sortable( Sortable.NO )
			)
					.toReference();
			string = root.field(
					"string",
					f -> f.asString().projectable( Projectable.YES ).sortable( Sortable.YES )
			)
					.toReference();
		}
	}

	protected static class UnsearchableFieldsIndexBinding {
		final IndexFieldReference<GeoPoint> geoPoint;

		UnsearchableFieldsIndexBinding(IndexSchemaElement root) {
			geoPoint = root.field(
					// make the field not searchable
					"geoPoint", f -> f.asGeoPoint().searchable( Searchable.NO )
			)
					.toReference();
		}
	}
}
