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
import org.hibernate.search.engine.logging.spi.FailureContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.RangeBoundInclusion;
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

public class RangeSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY_ID = "empty";

	// Document 1

	private static final String STRING_1 = "ccc";
	private static final Integer INTEGER_1 = 3;
	private static final LocalDate LOCAL_DATE_1 = LocalDate.of( 2003, 6, 3 );

	private static final String ONE_MORE_STRING_1 = "ddd";
	private static final String EVEN_MORE_STRING_1 = "eee";

	// Document 2

	private static final String STRING_2 = "mmm";
	private static final Integer INTEGER_2 = 13;
	private static final LocalDate LOCAL_DATE_2 = LocalDate.of( 2013, 6, 3 );

	private static final String ONE_MORE_STRING_2 = "nnn";
	private static final String EVEN_MORE_STRING_2 = "ooo";

	// Document 3

	private static final String STRING_3 = "xxx";
	private static final Integer INTEGER_3 = 25;
	private static final LocalDate LOCAL_DATE_3 = LocalDate.of( 2025, 6, 3 );

	private static final String ONE_MORE_STRING_3 = "yyy";
	private static final String EVEN_MORE_STRING_3 = "zzz";

	// GeoPoint is not here as we don't support range predicates for GeoPoint
	private static final List<String> FIELDS = Arrays.asList( "string", "integer", "localDate" );
	private static final List<Object> VALUES_1 = Arrays.asList( STRING_1, INTEGER_1, LOCAL_DATE_1 );
	private static final List<Object> LOWER_VALUES_TO_MATCH = Arrays.asList( "ggg", 10, LocalDate.of( 2010, 6, 8 ) );
	private static final List<Object> VALUES_2 = Arrays.asList( STRING_2, INTEGER_2, LOCAL_DATE_2 );
	private static final List<Object> UPPER_VALUES_TO_MATCH = Arrays.asList( "rrr", 19, LocalDate.of( 2019, 4, 18 ) );
	private static final List<Object> VALUES_3 = Arrays.asList( STRING_3, INTEGER_3, LOCAL_DATE_3 );

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
	public void above() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < FIELDS.size(); i++ ) {
			String absoluteFieldPath = FIELDS.get( i );
			Object lowerValueToMatch = LOWER_VALUES_TO_MATCH.get( i );

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).above( lowerValueToMatch )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
		}
	}

	@Test
	public void above_include_exclude() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < FIELDS.size(); i++ ) {
			String absoluteFieldPath = FIELDS.get( i );
			Object lowerValueToMatch = VALUES_2.get( i );

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).above( lowerValueToMatch )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

			// explicit inclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).above( lowerValueToMatch, RangeBoundInclusion.INCLUDED )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

			// explicit exclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).above( lowerValueToMatch, RangeBoundInclusion.EXCLUDED )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
		}
	}

	@Test
	public void below() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < FIELDS.size(); i++ ) {
			String absoluteFieldPath = FIELDS.get( i );
			Object upperValueToMatch = UPPER_VALUES_TO_MATCH.get( i );

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).below( upperValueToMatch )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	@Test
	public void below_include_exclude() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < FIELDS.size(); i++ ) {
			String absoluteFieldPath = FIELDS.get( i );
			Object upperValueToMatch = VALUES_2.get( i );

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).below( upperValueToMatch )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit inclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).below( upperValueToMatch, RangeBoundInclusion.INCLUDED )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit exclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).below( upperValueToMatch, RangeBoundInclusion.EXCLUDED )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}
	}

	@Test
	public void from_to() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < FIELDS.size(); i++ ) {
			String absoluteFieldPath = FIELDS.get( i );
			Object lowerValueToMatch = LOWER_VALUES_TO_MATCH.get( i );
			Object upperValueToMatch = UPPER_VALUES_TO_MATCH.get( i );

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).from( lowerValueToMatch ).to( upperValueToMatch )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void from_to_include_exclude() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < FIELDS.size(); i++ ) {
			String absoluteFieldPath = FIELDS.get( i );
			Object value1ToMatch = VALUES_1.get( i );
			Object value2ToMatch = VALUES_2.get( i );
			Object value3ToMatch = VALUES_3.get( i );

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath ).from( value1ToMatch ).to( value2ToMatch )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit inclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath )
							.from( value1ToMatch, RangeBoundInclusion.INCLUDED )
							.to( value2ToMatch, RangeBoundInclusion.INCLUDED )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit exclusion for the from clause

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath )
							.from( value1ToMatch, RangeBoundInclusion.EXCLUDED )
							.to( value2ToMatch, RangeBoundInclusion.INCLUDED )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

			// explicit exclusion for the to clause

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath )
							.from( value1ToMatch, RangeBoundInclusion.INCLUDED )
							.to( value2ToMatch, RangeBoundInclusion.EXCLUDED )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

			// explicit exclusion for both clauses

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate().range().onField( absoluteFieldPath )
							.from( value1ToMatch, RangeBoundInclusion.EXCLUDED )
							.to( value3ToMatch, RangeBoundInclusion.EXCLUDED )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void unsupported_field_types() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( int i = 0; i < UNSUPPORTED_FIELD_TYPE_PATHS.size(); i++ ) {
			String absoluteFieldPath = UNSUPPORTED_FIELD_TYPE_PATHS.get( i );
			Object lowerValueToMatch = UNSUPPORTED_FIELD_TYPE_VALUES.get( i );

			SubTest.expectException(
					"range() predicate with unsupported type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath ).above( lowerValueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Range predicates are not supported by" )
					.satisfies( FailureReportUtils.hasContext(
							FailureContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void boost() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().bool()
						.should().range().onField( "string" ).above( STRING_3 )
						.should().range().onField( "string" ).boostedTo( 42 ).below( STRING_1 )
				.end()
				.sort().byScore().end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().bool()
						.should().range().onField( "string" ).boostedTo( 42 ).above( STRING_3 )
						.should().range().onField( "string" ).below( STRING_1 )
				.end()
				.sort().byScore().end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );
	}

	@Test
	public void multi_fields() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().range().onField( "string" ).orField( "one_more_string" ).below( STRING_1 )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().range().onField( "string" ).orField( "one_more_string" ).above( ONE_MORE_STRING_3 )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onField().orFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().range().onField( "string" ).orFields( "one_more_string", "even_more_string" ).below( STRING_1 )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().range().onField( "string" ).orFields( "one_more_string", "even_more_string" ).from( "d" ).to( "e" )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().range().onField( "string" ).orFields( "one_more_string", "even_more_string" ).above( EVEN_MORE_STRING_3 )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().range().onFields( "string", "one_more_string" ).below( STRING_1 )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().range().onFields( "string", "one_more_string" ).above( ONE_MORE_STRING_3 )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void range_error_null() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( String fieldPath : FIELDS ) {
			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).from( null ).to( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );

			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).above( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );


			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).below( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void unknown_field() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onFields( "string", "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( "string" ).orField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( "string" ).orFields( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	private void initData() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( sessionContext );
		worker.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexAccessors.string.write( document, STRING_1 );
			indexAccessors.integer.write( document, INTEGER_1 );
			indexAccessors.localDate.write( document, LOCAL_DATE_1 );

			indexAccessors.one_more_string.write( document, ONE_MORE_STRING_1 );
			indexAccessors.even_more_string.write( document, EVEN_MORE_STRING_1 );
		} );
		worker.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexAccessors.string.write( document, STRING_2 );
			indexAccessors.integer.write( document, INTEGER_2 );
			indexAccessors.localDate.write( document, LOCAL_DATE_2 );

			indexAccessors.one_more_string.write( document, ONE_MORE_STRING_2 );
			indexAccessors.even_more_string.write( document, EVEN_MORE_STRING_2 );
		} );
		worker.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexAccessors.string.write( document, STRING_3 );
			indexAccessors.integer.write( document, INTEGER_3 );
			indexAccessors.localDate.write( document, LOCAL_DATE_3 );

			indexAccessors.one_more_string.write( document, ONE_MORE_STRING_3 );
			indexAccessors.even_more_string.write( document, EVEN_MORE_STRING_3 );
		} );
		worker.add( referenceProvider( EMPTY_ID ), document -> { } );

		worker.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<LocalDate> localDate;
		@SuppressWarnings("unused")
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
