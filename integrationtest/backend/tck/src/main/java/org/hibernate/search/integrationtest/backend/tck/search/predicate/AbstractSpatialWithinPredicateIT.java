/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractSpatialWithinPredicateIT {

	protected static final String OURSON_QUI_BOIT_ID = "ourson qui boit";
	protected static final GeoPoint OURSON_QUI_BOIT_GEO_POINT = GeoPoint.of( 45.7705687, 4.835233 );
	protected static final String OURSON_QUI_BOIT_STRING = "L'ourson qui boit";

	protected static final String IMOUTO_ID = "imouto";
	protected static final GeoPoint IMOUTO_GEO_POINT = GeoPoint.of( 45.7541719, 4.8386221 );
	protected static final String IMOUTO_STRING = "Imouto";

	protected static final String CHEZ_MARGOTTE_ID = "chez margotte";
	protected static final GeoPoint CHEZ_MARGOTTE_GEO_POINT = GeoPoint.of( 45.7530374, 4.8510299 );
	protected static final String CHEZ_MARGOTTE_STRING = "Chez Margotte";

	protected static final String EMPTY_ID = "empty";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	protected final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	protected final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	// TODO HSEARCH-3593 test incompatibilities ( projection, sort and so on and so forth )
	protected final SimpleMappedIndex<UnsearchableFieldsIndexBinding> unsearchableFieldsIndex =
			SimpleMappedIndex.of( UnsearchableFieldsIndexBinding::new ).name( "unsearchableFields" );

	@BeforeEach
	void setup() {
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
				.add( EMPTY_ID, document -> {} )
				.join();
	}

	public static class IndexBinding {
		public final IndexFieldReference<GeoPoint> geoPoint;
		public final IndexFieldReference<GeoPoint> geoPoint_1;
		public final IndexFieldReference<GeoPoint> geoPoint_2;
		public final IndexFieldReference<GeoPoint> geoPoint_with_longName;
		public final IndexFieldReference<GeoPoint> nonProjectableGeoPoint;
		public final IndexFieldReference<GeoPoint> unsortableGeoPoint;
		public final IndexFieldReference<GeoPoint> projectableUnsortableGeoPoint;
		public final IndexFieldReference<String> string;

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
