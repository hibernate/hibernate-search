/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.spatial;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.Assume.assumeFalse;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DistanceSearchableSortableIT {

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
	public void searchableSortable() {
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.spatial().within().field( "searchableSortable" ).circle( METRO_GARIBALDI, 1_500 ) )
				.sort( f -> f.distance( "searchableSortable", METRO_GARIBALDI ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void searchableNotSortable() {
		assumeFalse(
				"Skipping test for ES GeoPoint as those would become sortable by default in this case.",
				TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
		);
		StubMappingScope scope = index.createScope();
		String fieldPath = "searchableNotSortable";

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.spatial().within().field( fieldPath ).circle( METRO_GARIBALDI, 1_500 ) )
				.sort( f -> f.distance( fieldPath, METRO_GARIBALDI ) )
				.toQuery()

		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'sort:distance' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.spatial().within().field( fieldPath ).circle( METRO_GARIBALDI, 1_500 ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void searchableNotSortableNotProjectable() {
		StubMappingScope scope = index.createScope();
		String fieldPath = "searchableNotSortableNotProjectable";

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.spatial().within().field( fieldPath ).circle( METRO_GARIBALDI, 1_500 ) )
				.sort( f -> f.distance( fieldPath, METRO_GARIBALDI ) )
				.toQuery()

		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'sort:distance' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.spatial().within().field( fieldPath ).circle( METRO_GARIBALDI, 1_500 ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void searchableDefaultSortable() {
		assumeFalse(
				"Skipping test for ES GeoPoint as those would become sortable by default in this case.",
				TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
		);
		StubMappingScope scope = index.createScope();
		String fieldPath = "searchableDefaultSortable";

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.spatial().within().field( fieldPath ).circle( METRO_GARIBALDI, 1_500 ) )
				.sort( f -> f.distance( fieldPath, METRO_GARIBALDI ) )
				.toQuery()

		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'sort:distance' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.spatial().within().field( fieldPath ).circle( METRO_GARIBALDI, 1_500 ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	@Test
	public void notSearchableSortable() {
		StubMappingScope scope = index.createScope();
		String fieldPath = "notSearchableSortable";

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.spatial().within().field( fieldPath ).circle( METRO_GARIBALDI, 1_500 ) )
				.sort( f -> f.distance( fieldPath, METRO_GARIBALDI ) )
				.toQuery()

		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'predicate:spatial:within-circle' on field '" + fieldPath + "'"
				);

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.distance( fieldPath, METRO_GARIBALDI ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID, OURSON_QUI_BOIT_ID );
	}

	@Test
	public void defaultSearchableSortable() {
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.spatial().within().field( "defaultSearchableSortable" ).circle( METRO_GARIBALDI, 1_500 ) )
				.sort( f -> f.distance( "defaultSearchableSortable", METRO_GARIBALDI ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index.typeName(), CHEZ_MARGOTTE_ID, IMOUTO_ID );
	}

	private void initData() {
		index.bulkIndexer()
				.add( OURSON_QUI_BOIT_ID, document -> {
					document.addValue( index.binding().searchableSortable, OURSON_QUI_BOIT_GEO_POINT );
					document.addValue( index.binding().searchableNotSortable, OURSON_QUI_BOIT_GEO_POINT );
					document.addValue( index.binding().searchableNotSortableNotProjectable, OURSON_QUI_BOIT_GEO_POINT );
					document.addValue( index.binding().searchableDefaultSortable, OURSON_QUI_BOIT_GEO_POINT );
					document.addValue( index.binding().notSearchableSortable, OURSON_QUI_BOIT_GEO_POINT );
					document.addValue( index.binding().defaultSearchableSortable, OURSON_QUI_BOIT_GEO_POINT );
				} )
				.add( IMOUTO_ID, document -> {
					document.addValue( index.binding().searchableSortable, IMOUTO_GEO_POINT );
					document.addValue( index.binding().searchableNotSortable, IMOUTO_GEO_POINT );
					document.addValue( index.binding().searchableNotSortableNotProjectable, IMOUTO_GEO_POINT );
					document.addValue( index.binding().searchableDefaultSortable, IMOUTO_GEO_POINT );
					document.addValue( index.binding().notSearchableSortable, IMOUTO_GEO_POINT );
					document.addValue( index.binding().defaultSearchableSortable, IMOUTO_GEO_POINT );
				} )
				.add( CHEZ_MARGOTTE_ID, document -> {
					document.addValue( index.binding().searchableSortable, CHEZ_MARGOTTE_GEO_POINT );
					document.addValue( index.binding().searchableNotSortable, CHEZ_MARGOTTE_GEO_POINT );
					document.addValue( index.binding().searchableNotSortableNotProjectable, CHEZ_MARGOTTE_GEO_POINT );
					document.addValue( index.binding().searchableDefaultSortable, CHEZ_MARGOTTE_GEO_POINT );
					document.addValue( index.binding().notSearchableSortable, CHEZ_MARGOTTE_GEO_POINT );
					document.addValue( index.binding().defaultSearchableSortable, CHEZ_MARGOTTE_GEO_POINT );
				} )
				.join();
	}

	protected static class IndexBinding {
		final IndexFieldReference<GeoPoint> searchableSortable;
		final IndexFieldReference<GeoPoint> searchableNotSortable;
		final IndexFieldReference<GeoPoint> searchableNotSortableNotProjectable;
		final IndexFieldReference<GeoPoint> searchableDefaultSortable;
		final IndexFieldReference<GeoPoint> notSearchableSortable;
		final IndexFieldReference<GeoPoint> defaultSearchableSortable;

		IndexBinding(IndexSchemaElement root) {
			searchableSortable = root
					.field( "searchableSortable", f -> f.asGeoPoint().searchable( Searchable.YES ).sortable( Sortable.YES ) )
					.toReference();
			searchableNotSortable = root
					.field( "searchableNotSortable", f -> f.asGeoPoint().searchable( Searchable.YES ).sortable( Sortable.NO ) )
					.toReference();
			searchableNotSortableNotProjectable = root.field( "searchableNotSortableNotProjectable",
					f -> f.asGeoPoint().searchable( Searchable.YES ).sortable( Sortable.NO ).projectable( Projectable.NO ) )
					.toReference();
			searchableDefaultSortable =
					root.field( "searchableDefaultSortable", f -> f.asGeoPoint().searchable( Searchable.YES ) ).toReference();
			notSearchableSortable = root
					.field( "notSearchableSortable", f -> f.asGeoPoint().searchable( Searchable.NO ).sortable( Sortable.YES ) )
					.toReference();
			defaultSearchableSortable =
					root.field( "defaultSearchableSortable", f -> f.asGeoPoint().sortable( Sortable.YES ) ).toReference();
		}
	}

}
