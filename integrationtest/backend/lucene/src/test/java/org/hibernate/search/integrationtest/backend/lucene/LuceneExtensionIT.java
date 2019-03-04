/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.assertj.core.api.Assertions;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LuceneExtensionIT {

	private static final String BACKEND_NAME = "myLuceneBackend";
	private static final String INDEX_NAME = "IndexName";

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String FOURTH_ID = "4";
	private static final String FIFTH_ID = "5";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SearchIntegration integration;
	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		this.integration = setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void predicate_fromLuceneQuery() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.extension( LuceneExtension.get() )
								.fromLuceneQuery( new TermQuery( new Term( "string", "text 1" ) ) )
						)
						.should( f.extension( LuceneExtension.get() )
								.fromLuceneQuery( IntPoint.newExactQuery( "integer", 2 ) )
						)
						.should( f.extension( LuceneExtension.get() )
								.fromLuceneQuery( LatLonPoint.newDistanceQuery( "geoPoint", 40, -70, 200_000 ) )
						)
				)
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID )
				.hasHitCount( 3 );
	}

	@Test
	public void predicate_fromLuceneQuery_separatePredicate() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchPredicate predicate1 = searchTarget.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( new TermQuery( new Term( "string", "text 1" ) ) ).toPredicate();
		SearchPredicate predicate2 = searchTarget.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( IntPoint.newExactQuery( "integer", 2 ) ).toPredicate();
		SearchPredicate predicate3 = searchTarget.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( LatLonPoint.newDistanceQuery( "geoPoint", 40, -70, 200_000 ) ).toPredicate();
		SearchPredicate booleanPredicate = searchTarget.predicate().bool()
				.should( predicate1 )
				.should( predicate2 )
				.should( predicate3 )
				.toPredicate();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( booleanPredicate )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID )
				.hasHitCount( 3 );
	}

	@Test
	public void sort_fromLuceneSortField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c
						.extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort1", Type.STRING ) )
						.then().extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort2", Type.STRING ) )
						.then().extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort3", Type.STRING ) )
				)
				.build();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
		);

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c
						.extension().ifSupported(
								LuceneExtension.get(),
								c2 -> c2.fromLuceneSort( new Sort(
										new SortField( "sort3", Type.STRING ),
										new SortField( "sort2", Type.STRING ),
										new SortField( "sort1", Type.STRING )
									)
								)
						)
				)
				.build();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				THIRD_ID, SECOND_ID, FIRST_ID, FOURTH_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_fromLuceneSortField_separateSort() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchSort sort1 = searchTarget.sort().extension()
						.ifSupported(
								LuceneExtension.get(),
								c2 -> c2.fromLuceneSortField( new SortField( "sort1", Type.STRING ) )
						)
						.orElseFail()
				.toSort();
		SearchSort sort2 = searchTarget.sort().extension( LuceneExtension.get() )
				.fromLuceneSortField( new SortField( "sort2", Type.STRING ) )
				.toSort();
		SearchSort sort3 = searchTarget.sort().extension()
				.ifSupported(
						LuceneExtension.get(),
						c2 -> c2.fromLuceneSortField( new SortField( "sort3", Type.STRING ) )
				)
				.orElseFail()
				.toSort();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sort1 ).then().by( sort2 ).then().by( sort3 ) )
				.build();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID );

		SearchSort sort = searchTarget.sort()
				.extension( LuceneExtension.get() ).fromLuceneSort( new Sort(
						new SortField( "sort3", Type.STRING ),
						new SortField( "sort2", Type.STRING ),
						new SortField( "sort1", Type.STRING )
					)
				)
				.toSort();

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sort ) )
				.build();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, FOURTH_ID, FIFTH_ID );
	}

	@Test
	public void predicate_nativeField_throwsException() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SubTest.expectException(
				"match() predicate on unsupported native field",
				() -> searchTarget.query()
						.asReference()
						.predicate( f -> f.match().onField( "nativeField" ).matching( "37" ) )
						.build()
				)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Native fields do not support defining predicates with the DSL: use the Lucene extension and a native query." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField" )
				) );
	}

	@Test
	public void sort_nativeField_throwsException() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SubTest.expectException(
				"sort on unsupported native field",
				() -> searchTarget.query()
						.asReference()
						.predicate( f -> f.matchAll() )
						.sort( c -> c.byField( "nativeField" ) )
						.build()
				)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Native fields do not support defining sorts with the DSL: use the Lucene extension and a native sort." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField" )
				) );
	}

	@Test
	public void predicate_nativeField_nativeQuery() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.extension( LuceneExtension.get() )
						.fromLuceneQuery( new TermQuery( new Term( "nativeField", "37" ) ) )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID );
	}

	@Test
	public void projection_nativeField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<Integer> query = searchTarget.query()
				.asProjection( f -> f.field( "nativeField", Integer.class ) )
				.predicate( f -> f.match().onField( "string" ).matching( "text 1" ) )
				.build();

		assertThat( query ).hasHitsAnyOrder( 37 );
	}

	@Test
	public void projection_nativeField_unsupportedProjection() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		// let's check that it's possible to query the field beforehand
		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.extension( LuceneExtension.get() )
						.fromLuceneQuery( new TermQuery( new Term( "nativeField_unsupportedProjection", "37" ) ) )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID );

		// now, let's check that projecting on the field throws an exception
		SubTest.expectException(
				"projection on native field not supporting projections",
				() -> searchTarget.projection().field( "nativeField_unsupportedProjection", Integer.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Projections are not enabled for field" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField_unsupportedProjection" )
				) );
	}

	@Test
	public void sort_nativeField_nativeSort() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.extension( LuceneExtension.get() ).fromLuceneSortField( new SortField( "nativeField", Type.LONG ) ) )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, FIRST_ID, FIFTH_ID, SECOND_ID, FOURTH_ID );
	}

	@Test
	public void projection_document() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<Document> query = searchTarget.query()
				.asProjection(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.predicate( f -> f.matchAll() )
				.build();

		List<Document> result = query.execute().getHits();
		Assertions.assertThat( result )
				.hasSize( 5 )
				.satisfies( containsDocument(
						FIRST_ID,
						doc -> doc.hasField( "string", "text 1" )
								.hasField( "nativeField", "37" )
								.hasField( "nativeField_unsupportedProjection", "37" )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						SECOND_ID,
						doc -> doc.hasField( "integer", 2 )
								.hasField( "nativeField", "78" )
								.hasField( "nativeField_unsupportedProjection", "78" )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						THIRD_ID,
						doc -> doc.hasField( "nativeField", "13" )
								.hasField( "nativeField_unsupportedProjection", "13" )
								// Geo points are stored as two internal fields
								.hasInternalField( "geoPoint_latitude", 40.12 )
								.hasInternalField( "geoPoint_longitude", -71.34 )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						FOURTH_ID,
						doc -> doc.hasField( "nativeField", "89" )
								.hasField( "nativeField_unsupportedProjection", "89" )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						FIFTH_ID,
						doc -> doc.hasField( "string", "text 2" )
								.hasField( "integer", 1 )
								.hasField( "nativeField", "53" )
								.hasField( "nativeField_unsupportedProjection", "53" )
								// Geo points are stored as two internal fields
								.hasInternalField( "geoPoint_latitude", 45.12 )
								.hasInternalField( "geoPoint_longitude", -75.34 )
								.andOnlyInternalFields()
				) );
	}

	/**
	 * Check that the projection on a document includes all fields,
	 * even if there is a field projection, which would usually trigger document filtering.
	 */
	@Test
	public void projection_documentAndField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<List<?>> query = searchTarget.query()
				.asProjection( f ->
						f.composite(
								f.extension( LuceneExtension.get() ).document(),
								f.field( "string" )
						)
				)
				.predicate( f -> f.id().matching( FIRST_ID ) )
				.build();

		List<Document> result = query.execute().getHits().stream()
				.map( list -> (Document) list.get( 0 ) )
				.collect( Collectors.toList() );
		Assertions.assertThat( result )
				.hasSize( 1 )
				.satisfies( containsDocument(
						FIRST_ID,
						doc -> doc.hasField( "string", "text 1" )
								.hasField( "nativeField", "37" )
								.hasField( "nativeField_unsupportedProjection", "37" )
								.andOnlyInternalFields()
				) );
	}

	@Test
	public void projection_explanation() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<Explanation> query = searchTarget.query()
				.asProjection( f -> f.extension( LuceneExtension.get() ).explanation() )
				.predicate( f -> f.id().matching( FIRST_ID ) )
				.build();

		List<Explanation> result = query.execute().getHits();
		Assertions.assertThat( result ).hasSize( 1 );
		Assertions.assertThat( result.get( 0 ) ).isInstanceOf( Explanation.class );
		Assertions.assertThat( result.get( 0 ).toString() )
				.contains( LuceneFields.idFieldName() );
	}

	@Test
	public void nativeField_invalidFieldPath() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();

		SubTest.expectException(
				"native field contributing field with invalid field path",
				() -> workPlan.add( referenceProvider( FIRST_ID ), document -> {
					indexAccessors.nativeField_invalidFieldPath.write( document, 45 );
				} ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid field path; expected path 'nativeField_invalidFieldPath', got 'not the expected path'." );
	}

	@Test
	public void backend_unwrap() {
		Backend backend = integration.getBackend( BACKEND_NAME );
		Assertions.assertThat( backend.unwrap( LuceneBackend.class ) )
				.isNotNull();
	}

	@Test
	public void backend_unwrap_error_unknownType() {
		Backend backend = integration.getBackend( BACKEND_NAME );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Attempt to unwrap a Lucene backend to '" + String.class.getName() + "'" );
		thrown.expectMessage( "this backend can only be unwrapped to '" + LuceneBackend.class.getName() + "'" );

		backend.unwrap( String.class );
	}

	@Test
	public void indexManager_unwrap() {
		IndexManager indexManager = integration.getIndexManager( INDEX_NAME );
		Assertions.assertThat( indexManager.unwrap( LuceneIndexManager.class ) )
				.isNotNull();
	}

	@Test
	public void indexManager_unwrap_error_unknownType() {
		IndexManager indexManager = integration.getIndexManager( INDEX_NAME );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Attempt to unwrap a Lucene index manager to '" + String.class.getName() + "'" );
		thrown.expectMessage( "this index manager can only be unwrapped to '" + LuceneIndexManager.class.getName() + "'" );

		indexManager.unwrap( String.class );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( FIRST_ID ), document -> {
			indexAccessors.string.write( document, "text 1" );

			indexAccessors.nativeField.write( document, 37 );
			indexAccessors.nativeField_unsupportedProjection.write( document, 37 );

			indexAccessors.sort1.write( document, "a" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "z" );
		} );
		workPlan.add( referenceProvider( SECOND_ID ), document -> {
			indexAccessors.integer.write( document, 2 );

			indexAccessors.nativeField.write( document, 78 );
			indexAccessors.nativeField_unsupportedProjection.write( document, 78 );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "a" );
			indexAccessors.sort3.write( document, "z" );
		} );
		workPlan.add( referenceProvider( THIRD_ID ), document -> {
			indexAccessors.geoPoint.write( document, GeoPoint.of( 40.12, -71.34 ) );

			indexAccessors.nativeField.write( document, 13 );
			indexAccessors.nativeField_unsupportedProjection.write( document, 13 );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "a" );
		} );
		workPlan.add( referenceProvider( FOURTH_ID ), document -> {
			indexAccessors.nativeField.write( document, 89 );
			indexAccessors.nativeField_unsupportedProjection.write( document, 89 );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "z" );
		} );
		workPlan.add( referenceProvider( FIFTH_ID ), document -> {
			// This document should not match any query
			indexAccessors.string.write( document, "text 2" );
			indexAccessors.integer.write( document, 1 );
			indexAccessors.geoPoint.write( document, GeoPoint.of( 45.12, -75.34 ) );

			indexAccessors.nativeField.write( document, 53 );
			indexAccessors.nativeField_unsupportedProjection.write( document, 53 );

			indexAccessors.sort1.write( document, "zz" );
			indexAccessors.sort2.write( document, "zz" );
			indexAccessors.sort3.write( document, "zz" );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
		);
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<GeoPoint> geoPoint;
		final IndexFieldAccessor<Integer> nativeField;
		final IndexFieldAccessor<Integer> nativeField_unsupportedProjection;
		final IndexFieldAccessor<Integer> nativeField_invalidFieldPath;

		final IndexFieldAccessor<String> sort1;
		final IndexFieldAccessor<String> sort2;
		final IndexFieldAccessor<String> sort3;

		IndexAccessors(IndexSchemaElement root) {
			integer = root.field(
					"integer",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.createAccessor();
			string = root.field(
					"string",
					f -> f.asString().projectable( Projectable.YES )
			)
					.createAccessor();
			geoPoint = root.field(
					"geoPoint",
					f -> f.asGeoPoint().projectable( Projectable.YES )
			)
					.createAccessor();
			nativeField = root.field(
					"nativeField",
					f -> f.extension( LuceneExtension.get() )
							.asLuceneField( Integer.class, LuceneExtensionIT::contributeNativeField, LuceneExtensionIT::fromNativeField )
			)
					.createAccessor();
			nativeField_unsupportedProjection = root.field(
					"nativeField_unsupportedProjection",
					f -> f.extension( LuceneExtension.get() )
							.asLuceneField( Integer.class, LuceneExtensionIT::contributeNativeField )
			)
					.createAccessor();
			nativeField_invalidFieldPath = root.field(
					"nativeField_invalidFieldPath",
					f -> f.extension( LuceneExtension.get() )
							.asLuceneField( Integer.class, LuceneExtensionIT::contributeNativeFieldInvalidFieldPath )
			)
					.createAccessor();

			sort1 = root.field( "sort1", f -> f.asString().sortable( Sortable.YES ) )
					.createAccessor();
			sort2 = root.field( "sort2", f -> f.asString().sortable( Sortable.YES ) )
					.createAccessor();
			sort3 = root.field( "sort3", f -> f.asString().sortable( Sortable.YES ) )
					.createAccessor();
		}
	}

	private static void contributeNativeField(String absoluteFieldPath, Integer value, Consumer<IndexableField> collector) {
		collector.accept( new StringField( absoluteFieldPath, value.toString(), Store.YES ) );
		collector.accept( new NumericDocValuesField( absoluteFieldPath, value.longValue() ) );
	}

	private static Integer fromNativeField(IndexableField field) {
		return Integer.parseInt( field.stringValue() );
	}

	private static void contributeNativeFieldInvalidFieldPath(String absoluteFieldPath, Integer value, Consumer<IndexableField> collector) {
		collector.accept( new StringField( "not the expected path", value.toString(), Store.YES ) );
	}
}
