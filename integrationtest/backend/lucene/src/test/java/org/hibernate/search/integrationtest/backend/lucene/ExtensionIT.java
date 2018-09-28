/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.function.Consumer;

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.assertj.core.api.Assertions;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.assertion.ProjectionsSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.test.SubTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExtensionIT {

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
	private MappedIndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		this.integration = setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void predicate_fromLuceneQuery() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().bool( b -> {
					b.should().extension( LuceneExtension.get() )
							.fromLuceneQuery( new TermQuery( new Term( "string", "text 1" ) ) );
					b.should().extension( LuceneExtension.get() )
							.fromLuceneQuery( IntPoint.newExactQuery( "integer", 2 ) );
					b.should().extension( LuceneExtension.get() )
							.fromLuceneQuery( LatLonPoint.newDistanceQuery( "geoPoint", 40, -70, 200_000 ) );
				} )
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID )
				.hasHitCount( 3 );
	}

	@Test
	public void predicate_fromLuceneQuery_separatePredicate() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchPredicate predicate1 = searchTarget.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( new TermQuery( new Term( "string", "text 1" ) ) );
		SearchPredicate predicate2 = searchTarget.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( IntPoint.newExactQuery( "integer", 2 ) );
		SearchPredicate predicate3 = searchTarget.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( LatLonPoint.newDistanceQuery( "geoPoint", 40, -70, 200_000 ) );
		SearchPredicate booleanPredicate = searchTarget.predicate().bool( b -> {
			b.should( predicate1 );
			b.should( predicate2 );
			b.should( predicate3 );
		} );

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( booleanPredicate )
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID )
				.hasHitCount( 3 );
	}

	@Test
	public void sort_fromLuceneSortField() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort( c -> c
						.extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort1", Type.STRING ) )
						.then().extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort2", Type.STRING ) )
						.then().extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort3", Type.STRING ) )
				)
				.build();
		assertThat( query ).hasReferencesHitsExactOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
		);

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
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
		assertThat( query ).hasReferencesHitsExactOrder(
				INDEX_NAME,
				THIRD_ID, SECOND_ID, FIRST_ID, FOURTH_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_fromLuceneSortField_separateSort() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchSort sort1 = searchTarget.sort().extension()
						.ifSupported(
								LuceneExtension.get(),
								c2 -> c2.fromLuceneSortField( new SortField( "sort1", Type.STRING ) )
						)
						.orElseFail()
				.end();
		SearchSort sort2 = searchTarget.sort().extension( LuceneExtension.get() )
				.fromLuceneSortField( new SortField( "sort2", Type.STRING ) )
				.end();
		SearchSort sort3 = searchTarget.sort().extension()
				.ifSupported(
						LuceneExtension.get(),
						c2 -> c2.fromLuceneSortField( new SortField( "sort3", Type.STRING ) )
				)
				.orElseFail()
				.end();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().by( sort1 ).then().by( sort2 ).then().by( sort3 ).end()
				.build();
		assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID );

		SearchSort sort = searchTarget.sort()
				.extension( LuceneExtension.get() ).fromLuceneSort( new Sort(
						new SortField( "sort3", Type.STRING ),
						new SortField( "sort2", Type.STRING ),
						new SortField( "sort1", Type.STRING )
					)
				)
				.end();

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().by( sort ).end()
				.build();
		assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, FOURTH_ID, FIFTH_ID );
	}

	@Test
	public void predicate_nativeField_throwsException() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"match() predicate on unsupported native field",
				() -> searchTarget.query( sessionContext )
						.asReferences()
						.predicate()
								.match().onField( "nativeField" ).matching( "37" ).end()
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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"sort on unsupported native field",
				() -> searchTarget.query( sessionContext )
						.asReferences()
						.predicate().matchAll().end()
						.sort().byField( "nativeField" ).end()
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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate()
						.extension( LuceneExtension.get() ).fromLuceneQuery( new TermQuery( new Term( "nativeField", "37" ) ) )
				.build();

		assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, FIRST_ID );
	}

	@Test
	public void projection_nativeField() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<List<?>> query = searchTarget.query( sessionContext )
				.asProjections( searchTarget.projection().field( "nativeField", Integer.class ).toProjection() )
				.predicate().match().onField( "string" ).matching( "text 1" ).end()
				.build();

		ProjectionsSearchResultAssert.assertThat( query ).hasProjectionsHitsAnyOrder( c -> {
			c.projection( 37 );
		} );
	}

	@Test
	public void projection_nativeField_unsupportedProjection() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// let's check that it's possible to query the field beforehand
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate()
						.extension( LuceneExtension.get() ).fromLuceneQuery( new TermQuery( new Term( "nativeField_unsupportedProjection", "37" ) ) )
				.build();

		assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, FIRST_ID );

		// now, let's check that projecting on the field throws an exception
		SubTest.expectException(
				"projection on native field not supporting projections",
				() -> {
						SearchQuery<List<?>> projectionQuery = searchTarget.query( sessionContext )
								.asProjections( searchTarget.projection().field( "nativeField_unsupportedProjection", Integer.class ).toProjection() )
								.predicate().matchAll().end()
								.build();
						projectionQuery.execute();
				} )
				.assertThrown()
				.hasCauseInstanceOf( SearchException.class )
				.hasMessageContaining( "This native field does not support projection." )
				.satisfies( FailureReportUtils.hasCauseWithContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField_unsupportedProjection" )
				) );
	}

	@Test
	public void predicate_nativeField_nativeSort() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().extension( LuceneExtension.get() ).fromLuceneSortField( new SortField( "nativeField", Type.LONG ) ).end()
				.build();

		assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, THIRD_ID, FIRST_ID, FIFTH_ID, SECOND_ID, FOURTH_ID );
	}

	@Test
	public void nativeField_invalidFieldPath() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );

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
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder(
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
			integer = root.field( "integer" )
					.asInteger()
					.createAccessor();
			string = root.field( "string" )
					.asString()
					.createAccessor();
			geoPoint = root.field( "geoPoint" )
					.asGeoPoint()
					.createAccessor();
			nativeField = root.field( "nativeField" )
					.extension( LuceneExtension.get() )
					.asLuceneField( Integer.class, ExtensionIT::contributeNativeField, ExtensionIT::fromNativeField )
					.createAccessor();
			nativeField_unsupportedProjection = root.field( "nativeField_unsupportedProjection" )
					.extension( LuceneExtension.get() )
					.asLuceneField( Integer.class, ExtensionIT::contributeNativeField )
					.createAccessor();
			nativeField_invalidFieldPath = root.field( "nativeField_invalidFieldPath" )
					.extension( LuceneExtension.get() )
					.asLuceneField( Integer.class, ExtensionIT::contributeNativeFieldInvalidFieldPath )
					.createAccessor();

			sort1 = root.field( "sort1" )
					.asString()
					.sortable( Sortable.YES )
					.createAccessor();
			sort2 = root.field( "sort2" )
					.asString()
					.sortable( Sortable.YES )
					.createAccessor();
			sort3 = root.field( "sort3" )
					.asString()
					.sortable( Sortable.YES )
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
