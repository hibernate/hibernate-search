/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.ImmutableGeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MatchSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String MATCHING_ID = "matching";
	private static final String NON_MATCHING_ID = "nonMatching";
	private static final String EMPTY_ID = "empty";
	private static final String ADDITIONAL_MATCHING_ID = "additional_matching";

	private static final String MATCHING_STRING = "Irving";
	private static final Integer MATCHING_INTEGER = 42;
	private static final LocalDate MATCHING_LOCAL_DATE = LocalDate.of( 1980, 10, 11 );
	private static final GeoPoint MATCHING_GEO_POINT = new ImmutableGeoPoint( 40, 70 );

	private static final String MATCHING_ONE_MORE_STRING = "Avenue of mysteries";
	private static final String MATCHING_EVEN_MORE_STRING = "Oto oto";

	private static final String NON_MATCHING_STRING = "Auster";
	private static final Integer NON_MATCHING_INTEGER = 67;
	private static final LocalDate NON_MATCHING_LOCAL_DATE = LocalDate.of( 1984, 10, 7 );
	private static final GeoPoint NON_MATCHING_GEO_POINT = new ImmutableGeoPoint( 45, 98 );

	private static final String NON_MATCHING_ONE_MORE_STRING = "4 3 2 1";
	private static final String NON_MATCHING_EVEN_MORE_STRING = "Doma";

	private static final String ADDITIONAL_MATCHING_STRING = "Coe";

	private static final List<String> FIELDS = Arrays.asList( "string", "integer", "localDate" );
	private static final List<Object> VALUES_TO_MATCH = Arrays.asList( MATCHING_STRING, MATCHING_INTEGER, MATCHING_LOCAL_DATE );

	// unsupported field types
	private static final List<String> UNSUPPORTED_FIELD_TYPE_PATHS = Arrays.asList( "geoPoint" );
	private static final List<Object> UNSUPPORTED_FIELD_TYPE_VALUES = Arrays.asList( new ImmutableGeoPoint( 40, 70 ) );

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

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

	@Test
	public void match() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < FIELDS.size(); i++ ) {
			String absoluteFieldPath = FIELDS.get( i );
			Object valueToMatch = VALUES_TO_MATCH.get( i );

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().match().onField( absoluteFieldPath ).matching( valueToMatch ).end()
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID );
		}
	}

	@Test
	public void unsupported_field_types() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < UNSUPPORTED_FIELD_TYPE_PATHS.size(); i++ ) {
			String absoluteFieldPath = UNSUPPORTED_FIELD_TYPE_PATHS.get( i );
			Object valueToMatch = UNSUPPORTED_FIELD_TYPE_VALUES.get( i );

			SubTest.expectException(
					"match() predicate with unsupported type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().match().onField( absoluteFieldPath ).matching( valueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Match predicates are not supported by" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void match_error_null() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( String fieldPath : FIELDS ) {
			SubTest.expectException(
					"matching() predicate with null value to match on field " + fieldPath,
					() -> searchTarget.predicate().match().onField( fieldPath ).matching( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "value to match" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void boost() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().bool()
						.should().match().onField( "string" ).matching( MATCHING_STRING ).end()
						.should().match().onField( "string" ).boostedTo( 42 ).matching( ADDITIONAL_MATCHING_STRING ).end()
				.end()
				.sort().byScore().end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, ADDITIONAL_MATCHING_ID, MATCHING_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().bool()
						.should().match().onField( "string" ).boostedTo( 42 ).matching( MATCHING_STRING ).end()
						.should().match().onField( "string" ).matching( ADDITIONAL_MATCHING_STRING ).end()
				.end()
				.sort().byScore().end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, MATCHING_ID, ADDITIONAL_MATCHING_ID );
	}

	@Test
	public void multi_fields() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().match().onField( "string" ).orField( "one_more_string" ).matching( MATCHING_STRING ).end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().match().onField( "string" ).orField( "one_more_string" ).matching( MATCHING_ONE_MORE_STRING ).end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID );

		// onField().orFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().match().onField( "string" ).orFields( "one_more_string", "even_more_string" ).matching( MATCHING_STRING ).end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().match().onField( "string" ).orFields( "one_more_string", "even_more_string" ).matching( MATCHING_ONE_MORE_STRING ).end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().match().onField( "string" ).orFields( "one_more_string", "even_more_string" ).matching( MATCHING_EVEN_MORE_STRING ).end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID );

		// onFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().match().onFields( "string", "one_more_string" ).matching( MATCHING_STRING ).end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().match().onFields( "string", "one_more_string" ).matching( MATCHING_ONE_MORE_STRING ).end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID );
	}

	@Test
	public void unknown_field() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"match() predicate with unknown field",
				() -> searchTarget.predicate().match().onField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"match() predicate with unknown field",
				() -> searchTarget.predicate().match().onFields( "string", "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"match() predicate with unknown field",
				() -> searchTarget.predicate().match().onField( "string" ).orField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"match() predicate with unknown field",
				() -> searchTarget.predicate().match().onField( "string" ).orFields( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	private void initData() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( sessionContext );
		worker.add( referenceProvider( MATCHING_ID ), document -> {
			indexAccessors.string.write( document, MATCHING_STRING );
			indexAccessors.integer.write( document, MATCHING_INTEGER );
			indexAccessors.localDate.write( document, MATCHING_LOCAL_DATE );
			indexAccessors.geoPoint.write( document, MATCHING_GEO_POINT );

			indexAccessors.one_more_string.write( document, MATCHING_ONE_MORE_STRING );
			indexAccessors.even_more_string.write( document, MATCHING_EVEN_MORE_STRING );
		} );
		worker.add( referenceProvider( NON_MATCHING_ID ), document -> {
			indexAccessors.string.write( document, NON_MATCHING_STRING );
			indexAccessors.integer.write( document, NON_MATCHING_INTEGER );
			indexAccessors.localDate.write( document, NON_MATCHING_LOCAL_DATE );
			indexAccessors.geoPoint.write( document, NON_MATCHING_GEO_POINT );

			indexAccessors.one_more_string.write( document, NON_MATCHING_ONE_MORE_STRING );
			indexAccessors.even_more_string.write( document, NON_MATCHING_EVEN_MORE_STRING );
		} );
		worker.add( referenceProvider( EMPTY_ID ), document -> { } );
		worker.add( referenceProvider( ADDITIONAL_MATCHING_ID ), document -> {
			indexAccessors.string.write( document, ADDITIONAL_MATCHING_STRING );
		} );

		worker.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, MATCHING_ID, NON_MATCHING_ID, EMPTY_ID, ADDITIONAL_MATCHING_ID );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<LocalDate> localDate;
		final IndexFieldAccessor<GeoPoint> geoPoint;

		final IndexFieldAccessor<String> one_more_string;
		final IndexFieldAccessor<String> even_more_string;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
			integer = root.field( "integer" ).asInteger().createAccessor();
			localDate = root.field( "localDate" ).asLocalDate().createAccessor();
			geoPoint = root.field( "geoPoint" ).asGeoPoint().createAccessor();

			one_more_string = root.field( "one_more_string" ).asString().createAccessor();
			even_more_string = root.field( "even_more_string" ).asString().createAccessor();
		}
	}
}
