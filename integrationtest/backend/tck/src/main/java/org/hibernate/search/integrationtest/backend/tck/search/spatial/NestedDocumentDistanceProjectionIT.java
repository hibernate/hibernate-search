/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;

/**
 * Tests distance projections on flattened and nested documents.
 */
public class NestedDocumentDistanceProjectionIT {

	private static final String OURSON_QUI_BOIT_ID = "ourson qui boit";
	private static final GeoPoint OURSON_QUI_BOIT_GEO_POINT = GeoPoint.of( 45.7705687, 4.835233 );

	private static final String IMOUTO_ID = "imouto";
	private static final GeoPoint IMOUTO_GEO_POINT = GeoPoint.of( 45.7541719, 4.8386221 );

	private static final String CHEZ_MARGOTTE_ID = "chez margotte";
	private static final GeoPoint CHEZ_MARGOTTE_GEO_POINT = GeoPoint.of( 45.7530374, 4.8510299 );

	private static final GeoPoint METRO_GARIBALDI = GeoPoint.of( 45.7515926, 4.8514779 );

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
		initData();
	}

	@Test
	public void distance_flattenedDocument() {
		StubMappingScope scope = index.createScope();
		List<Double> hits = scope.query()
				.select( f -> f.distance( "flattened.geoPoint", METRO_GARIBALDI ) )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "ordinal" ).desc() )
				.fetchAllHits();

		assertEquals( 3, hits.size() );
		checkResult( hits.get( 0 ), 164d, Offset.offset( 10d ) );
		checkResult( hits.get( 1 ), 1037d, Offset.offset( 10d ) );
		checkResult( hits.get( 2 ), 2457d, Offset.offset( 10d ) );
	}

	@Test
	public void distance_nestedDocument() {
		StubMappingScope scope = index.createScope();
		List<Double> hits = scope.query()
				.select( f -> f.distance( "nested.geoPoint", METRO_GARIBALDI ) )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "ordinal" ).desc() )
				.fetchAllHits();

		assertEquals( 3, hits.size() );
		checkResult( hits.get( 0 ), 164d, Offset.offset( 10d ) );
		checkResult( hits.get( 1 ), 1037d, Offset.offset( 10d ) );
		checkResult( hits.get( 2 ), 2457d, Offset.offset( 10d ) );
	}

	private void initData() {
		index.bulkIndexer()
				.add( OURSON_QUI_BOIT_ID, document -> {
					document.addValue( index.binding().ordinalField, 1 );

					DocumentElement nestedDocument = document.addObject( index.binding().nestedDocument );
					nestedDocument.addValue( index.binding().nestedGeoPoint, OURSON_QUI_BOIT_GEO_POINT );

					DocumentElement flattenedDocument = document.addObject( index.binding().flattenedDocument );
					flattenedDocument.addValue( index.binding().flattenedGeoPoint, OURSON_QUI_BOIT_GEO_POINT );
				} )
				.add( IMOUTO_ID, document -> {
					document.addValue( index.binding().ordinalField, 2 );

					DocumentElement nestedDocument = document.addObject( index.binding().nestedDocument );
					nestedDocument.addValue( index.binding().nestedGeoPoint, IMOUTO_GEO_POINT );

					DocumentElement flattenedDocument = document.addObject( index.binding().flattenedDocument );
					flattenedDocument.addValue( index.binding().flattenedGeoPoint, IMOUTO_GEO_POINT );
				} )
				.add( CHEZ_MARGOTTE_ID, document -> {
					document.addValue( index.binding().ordinalField, 3 );

					DocumentElement nestedDocument = document.addObject( index.binding().nestedDocument );
					nestedDocument.addValue( index.binding().nestedGeoPoint, CHEZ_MARGOTTE_GEO_POINT );

					DocumentElement flattenedDocument = document.addObject( index.binding().flattenedDocument );
					flattenedDocument.addValue( index.binding().flattenedGeoPoint, CHEZ_MARGOTTE_GEO_POINT );
				} )
				.join();
	}

	private void checkResult(Double actual, Double expected, Offset<Double> offset) {
		if ( expected == null ) {
			Assertions.assertThat( actual ).isNull();
		}
		else {
			Assertions.assertThat( actual ).isCloseTo( expected, offset );
		}
	}

	protected static class IndexBinding {
		final IndexFieldReference<Integer> ordinalField;

		final IndexObjectFieldReference nestedDocument;
		final IndexFieldReference<GeoPoint> nestedGeoPoint;

		final IndexObjectFieldReference flattenedDocument;
		final IndexFieldReference<GeoPoint> flattenedGeoPoint;

		IndexBinding(IndexSchemaElement root) {
			ordinalField = root.field( "ordinal", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();

			IndexSchemaObjectField nested = root.objectField( "nested", ObjectFieldStorage.NESTED );
			nestedDocument = nested.toReference();
			nestedGeoPoint = nested.field( "geoPoint", f -> f.asGeoPoint().projectable( Projectable.YES ) ).toReference();

			IndexSchemaObjectField flattened = root.objectField( "flattened", ObjectFieldStorage.FLATTENED );
			flattenedDocument = flattened.toReference();
			flattenedGeoPoint = flattened.field( "geoPoint", f -> f.asGeoPoint().projectable( Projectable.YES ) ).toReference();
		}
	}
}
