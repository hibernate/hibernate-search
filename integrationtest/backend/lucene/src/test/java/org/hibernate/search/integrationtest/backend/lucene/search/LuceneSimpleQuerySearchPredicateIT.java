/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LuceneSimpleQuerySearchPredicateIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( "1", doc -> {
					doc.addValue( "string", "1" );
					doc.addValue( "integer", 1 );
					doc.addValue( "integer2", 1 );
					doc.addValue( "instant", Instant.parse( "2000-01-01T01:01:01Z" ) );
					doc.addValue( "instant2", Instant.parse( "2000-01-01T01:01:01Z" ) );
					doc.addValue( "localDate", LocalDate.of( 2000, 1, 1 ) );
					doc.addValue( "localDate2", LocalDate.of( 2000, 1, 1 ) );
				} )
				.add( "2", doc -> {
					doc.addValue( "string", "2" );
					doc.addValue( "integer", 2 );
					doc.addValue( "integer2", 2 );
					doc.addValue( "instant", Instant.parse( "2000-02-02T02:02:02Z" ) );
					doc.addValue( "instant2", Instant.parse( "2000-02-02T02:02:02Z" ) );
					doc.addValue( "localDate", LocalDate.of( 2000, 2, 2 ) );
					doc.addValue( "localDate2", LocalDate.of( 2000, 2, 2 ) );
				} )
				.add( "3", doc -> {
					doc.addValue( "string", "3" );
					doc.addValue( "integer", 3 );
					doc.addValue( "integer2", 3 );
					doc.addValue( "instant", Instant.parse( "2000-03-03T03:03:03Z" ) );
					doc.addValue( "instant2", Instant.parse( "2000-03-03T03:03:03Z" ) );
					doc.addValue( "localDate", LocalDate.of( 2000, 3, 3 ) );
					doc.addValue( "localDate2", LocalDate.of( 2000, 3, 3 ) );
				} )
				.join();
	}

	@ParameterizedTest
	@MethodSource
	void simpleQueryString(String field, String value1, String value2, String noMatch, String unParsableValue) {
		StubMappingScope scope = index.createScope();

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching( value1 ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching( value1 + " | " + value2 )
								.defaultOperator( BooleanOperator.AND ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching(
								String.format( "(%s | %s) + -%s", value1, value2, value1 ) )
								.defaultOperator( BooleanOperator.AND ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "2" );

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching( "-" + noMatch ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "3" );

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching( noMatch ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching(
								String.format( "\"%s\"", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().fields( field, field + "2" ).matching(
								String.format( "\"%s\"", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( "\"%s %s\"", value1, value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( "\"%s %s\"~10", value1, value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( "%s~10", value1 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		//  "reason": "Can only use prefix queries on keyword, text and wildcard fields - not on [integer] which is of type [long]",
		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( "%s*", value1 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( "%s", unParsableValue ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

	}

	public static List<? extends Arguments> simpleQueryString() {
		// String field, String value1, String value2, String noMatch, String unParsableValue
		return List.of(
				Arguments.of( "integer", "1", "2", "100", "not-an-int" ),
				Arguments.of( "instant", "2000-01-01T01:01:01Z", "2000-02-02T02:02:02Z", "2222-02-02T02:02:02Z",
						"not-an-instant" ),
				Arguments.of( "localDate", "2000-01-01", "2000-02-02", "2222-02-02", "not-an-localDate" )
		);
	}

	@ParameterizedTest
	@MethodSource
	void queryString(String field, String value1, String value2, String noMatch, String unParsableValue) {
		StubMappingScope scope = index.createScope();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + "^10" ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + " || " + value2 )
								.defaultOperator( BooleanOperator.AND ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + " OR " + value2 )
								.defaultOperator( BooleanOperator.AND ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + " && " + value2 ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + " AND " + value2 ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field )
								.matching( String.format( "(%s OR %s) AND !%s", value1, value2, value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "2" );

		// ranges:
		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "[%s TO %s]", value1, value2 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "{%s TO %s}", value1, value2 ) ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "[%s TO %s}", value1, value2 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "{%s TO %s]", value1, value2 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "2" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "[%s TO *]", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "3" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "{%s TO *]", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "2", "3" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "[* TO %s]", value2 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "[* TO %s}", value2 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		// NOTE: must not without match -- just no results
		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( "-" + noMatch ) )
						.fetchAllHits()
		).isEmpty();
		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( "!" + noMatch ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( noMatch ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( "\"%s\"", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().fields( field, field + "2" )
								.matching( String.format( "\"%s\"", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( index.typeName(), "1" );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( "\"%s %s\"", value1, value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( "\"%s %s\"~10", value1, value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( "%s~10", value1 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		//  "reason": "Can only use prefix queries on keyword, text and wildcard fields - not on [integer] which is of type [long]",
		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( "%s*", value1 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( "/%s/", value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( "%s?", value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching(
						String.format( "%s", unParsableValue ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

	}

	public static List<? extends Arguments> queryString() {
		// String field, String value1, String value2, String noMatch, String unParsableValue
		return List.of(
				Arguments.of( "integer", "1", "2", "100", "not-an-int" ),
				Arguments.of( "instant", "2000-01-01T01\\:01\\:01Z", "2000-02-02T02\\:02\\:02Z", "2222-02-02T02\\:02\\:02Z",
						"not-an-instant" ),
				Arguments.of( "localDate", "2000-01-01", "2000-02-02", "2222-02-02", "not-an-localDate" )
		);
	}

	@Test
	void debug() {
		StubMappingScope scope = index.createScope();
		scope.query()
				.where( f -> f.queryString().field( "string" ).matching( "1 | 2" )
						.defaultOperator( BooleanOperator.AND ) )
				.fetchAllHits();
	}


	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<Integer> integer2;
		final IndexFieldReference<String> string;
		final IndexFieldReference<Instant> instant;
		final IndexFieldReference<Instant> instant2;
		final IndexFieldReference<LocalDate> localDate;
		final IndexFieldReference<LocalDate> localDate2;

		IndexBinding(IndexSchemaElement root) {
			this.integer = root.field( "integer", c -> c.asInteger() ).toReference();
			this.integer2 = root.field( "integer2", c -> c.asInteger() ).toReference();
			this.string = root.field( "string", c -> c.asString().analyzer( AnalyzerNames.DEFAULT ) ).toReference();
			this.instant = root.field( "instant", c -> c.asInstant() ).toReference();
			this.instant2 = root.field( "instant2", c -> c.asInstant() ).toReference();
			this.localDate = root.field( "localDate", c -> c.asLocalDate() ).toReference();
			this.localDate2 = root.field( "localDate2", c -> c.asLocalDate() ).toReference();
		}
	}
}
