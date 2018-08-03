/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.ImmutableGeoPoint;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.assertj.core.api.Assertions;

/**
 * Generic tests for sorts. More specific tests can be found in other classes, such as {@link SearchSortByFieldIT}.
 */
public class SearchSortIT {

	private static final String INDEX_NAME = "IndexName";

	private static final int INDEX_ORDER_CHECKS = 10;

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String EMPTY_ID = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexAccessors indexAccessors;
	private IndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

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

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext<SearchSort>> sortContributor) {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		return searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort( sortContributor )
				.build();
	}

	@Test
	public void byIndexOrder() {
		/*
		 * We don't really know in advance what the index order is, but we want it to be consistent.
		 * Thus we just test that the order stays the same over several calls.
		 */

		SearchQuery<DocumentReference> query = simpleQuery( b -> b.byIndexOrder() );
		SearchResult<DocumentReference> firstCallResult = query.execute();
		assertThat( firstCallResult ).fromQuery( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		List<DocumentReference> firstCallHits = firstCallResult.getHits();

		for ( int i = 0; i < INDEX_ORDER_CHECKS; ++i ) {
			// Rebuild the query to bypass any cache in the query object
			query = simpleQuery( b -> b.byIndexOrder() );
			SearchResultAssert.assertThat( query ).hasHitsExactOrder( firstCallHits );
		}
	}

	@Test
	public void nested() {
		Assume.assumeTrue( "Sorts on fields within nested fields are not supported yet", false );
		// TODO support sorts on fields within nested fields
	}

	@Test
	public void byScore() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query;

		SearchPredicate predicate = searchTarget.predicate()
				.match().onField( "string_analyzed_forScore" ).matching( "hooray" ).end();

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( predicate )
				.sort().byScore().end()
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( predicate )
				.sort().byScore().desc().end()
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( predicate )
				.sort().byScore().asc().end()
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID );
	}

	@Test
	public void separateSort() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query;

		SearchSort sort = searchTarget.sort()
				.byField( "string" ).asc().onMissingValue().sortLast()
				.end();

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort( sort )
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().by( sort ).end()
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		sort = searchTarget.sort()
				.byField( "string" ).desc().onMissingValue().sortLast()
				.end();

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort( sort )
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().by( sort ).end()
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
	}

	@Test
	public void lambda_caching() {
		AtomicReference<SearchSort> cache = new AtomicReference<>();

		Consumer<? super SearchSortContainerContext<SearchSort>> cachingContributor = c -> {
			if ( cache.get() == null ) {
				SearchSort result = c.byField( "string" ).onMissingValue().sortLast().end();
				cache.set( result );
			}
			else {
				c.by( cache.get() );
			}
		};

		Assertions.assertThat( cache ).hasValue( null );

		SearchQuery<DocumentReference> query;

		query = simpleQuery( cachingContributor );
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		Assertions.assertThat( cache ).doesNotHaveValue( null );

		query = simpleQuery( cachingContributor );
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	@Test
	public void byDistance() {
		SearchQuery<DocumentReference> query = simpleQuery( b -> b.byDistance( "geoPoint", new ImmutableGeoPoint( 45.757864, 4.834496 ) ) );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.byDistance( "geoPoint", 45.757864, 4.834496 ) );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		// we don't test the descending order here as it's currently not supported by Lucene
		// see the additional tests in the specific backend tests
	}

	private void initData() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( sessionContext );
		// Important: do not index the documents in the expected order after sorts
		worker.add( referenceProvider( SECOND_ID ), document -> {
			indexAccessors.string.write( document, "george" );
			indexAccessors.geoPoint.write( document, new ImmutableGeoPoint( 45.7705687,4.835233 ) );

			indexAccessors.string_analyzed_forScore.write( document, "Hooray Hooray" );
			indexAccessors.unsortable.write( document, "george" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.flattenedObject.string.write( flattenedObject, "george" );
			indexAccessors.flattenedObject.integer.write( flattenedObject, 2 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, "george" );
			indexAccessors.nestedObject.integer.write( nestedObject, 2 );
		} );
		worker.add( referenceProvider( FIRST_ID ), document -> {
			indexAccessors.string.write( document, "aaron" );
			indexAccessors.geoPoint.write( document, new ImmutableGeoPoint( 45.7541719, 4.8386221 ) );

			indexAccessors.string_analyzed_forScore.write( document, "Hooray Hooray Hooray" );
			indexAccessors.unsortable.write( document, "aaron" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.flattenedObject.string.write( flattenedObject, "aaron" );
			indexAccessors.flattenedObject.integer.write( flattenedObject, 1 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, "aaron" );
			indexAccessors.nestedObject.integer.write( nestedObject, 1 );
		} );
		worker.add( referenceProvider( THIRD_ID ), document -> {
			indexAccessors.string.write( document, "zach" );
			indexAccessors.geoPoint.write( document, new ImmutableGeoPoint( 45.7530374, 4.8510299 ) );

			indexAccessors.string_analyzed_forScore.write( document, "Hooray" );
			indexAccessors.unsortable.write( document, "zach" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.flattenedObject.string.write( flattenedObject, "zach" );
			indexAccessors.flattenedObject.integer.write( flattenedObject, 3 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, "zach" );
			indexAccessors.nestedObject.integer.write( nestedObject, 3 );
		} );
		worker.add( referenceProvider( EMPTY_ID ), document -> { } );

		worker.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<GeoPoint> geoPoint;
		final IndexFieldAccessor<String> string_analyzed_forScore;
		final IndexFieldAccessor<String> unsortable;

		final ObjectAccessors flattenedObject;
		final ObjectAccessors nestedObject;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string" ).asString().sortable( Sortable.YES ).createAccessor();
			geoPoint = root.field( "geoPoint" ).asGeoPoint().sortable( Sortable.YES ).createAccessor();
			string_analyzed_forScore = root.field( "string_analyzed_forScore" ).asString()
					.analyzer( "default" ).createAccessor();
			unsortable = root.field( "unsortable" ).asString().sortable( Sortable.NO ).createAccessor();

			IndexSchemaObjectField flattenedObjectField =
					root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new ObjectAccessors( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new ObjectAccessors( nestedObjectField );
		}
	}

	private static class ObjectAccessors {
		final IndexObjectFieldAccessor self;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<String> string;

		ObjectAccessors(IndexSchemaObjectField objectField) {
			self = objectField.createAccessor();
			string = objectField.field( "string" ).asString().sortable( Sortable.YES ).createAccessor();
			integer = objectField.field( "integer" ).asInteger().sortable( Sortable.YES ).createAccessor();
		}
	}
}
