/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;


public class SmokeIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void where_match() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.where( f -> f.match().field( "string_analyzed" ).matching( "text" ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "3" )
				.hasTotalHitCount( 3 );

		query = scope.query()
				.where( f -> f.match().field( "integer" ).matching( 1 ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.where( f -> f.match().field( "localDate" ).matching( LocalDate.of( 2018, 1, 1 ) ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.where( f -> f.match().field( "flattenedObject.string" ).matching( "text 1_1" ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void where_range() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.range().field( "string" ).between( "text 2", "text 42" ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.where( f -> f.range().field( "string_analyzed" ).between( "2", "42" ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.where( f -> f.range().field( "integer" ).between( 2, 42 ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.where( f -> f.range().field( "localDate" )
						.between( LocalDate.of( 2018, 1, 2 ),
								LocalDate.of( 2018, 2, 23 ) )
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "2" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.where( f -> f.range().field( "flattenedObject.integer" ).between( 201, 242 ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "2" )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void where_boolean() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( "integer" ).matching( 1 ) )
						.should( f.match().field( "integer" ).matching( 2 ) )
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "string_analyzed" ).matching( "text" ) )
						.filter( f.match().field( "integer" ).matching( 1 ) )
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "string_analyzed" ).matching( "text" ) )
						.mustNot( f.match().field( "integer" ).matching( 2 ) )
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "3" )
				.hasTotalHitCount( 2 );
	}

	@Test
	public void where_nested() {
		StubMappingScope scope = index.createScope();

		// Without nested structure, we expect predicates to be able to match on different objects
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "flattenedObject.string" ).matching( "text 1_2" ) )
						.must( f.match().field( "flattenedObject.integer" ).matching( 101 ) )
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" )
				.hasTotalHitCount( 1 );

//		TODO HSEARCH-3752 with implicit nested predicates, this is not true anymore:
//		// With nested structure, we expect direct queries to never match
//		query = scope.query()
//				.where( f -> f.match().field( "nestedObject.integer" ).matching( 101 ) )
//				.toQuery();
//		assertThat( query )
//				.hasNoHits()
//				.hasTotalHitCount( 0 );

		// ... and predicates within nested queries to be unable to match on different objects
		query = scope.query()
				.where( f -> f.nested( "nestedObject" )
						.must( f.match().field( "nestedObject.string" ).matching( "text 1_2" ) )
						.must( f.match().field( "nestedObject.integer" ).matching( 101 ) )
				)
				.toQuery();
		assertThatQuery( query )
				.hasNoHits()
				.hasTotalHitCount( 0 );

		// ... but predicates should still be able to match on the same object
		query = scope.query()
				.where( f -> f.nested( "nestedObject" )
						.must( f.match().field( "nestedObject.string" ).matching( "text 1_1" ) )
						.must( f.match().field( "nestedObject.integer" ).matching( 101 ) )
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void where_searchPredicate() {
		StubMappingScope scope = index.createScope();

		SearchPredicate predicate = scope.predicate().match().field( "string" ).matching( "text 1" ).toPredicate();
		SearchQuery<DocumentReference> query = scope.query()
				.where( predicate )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" )
				.hasTotalHitCount( 1 );

		predicate = scope.predicate().range().field( "integer" ).between( 1, 2 ).toPredicate();
		query = scope.query()
				.where( predicate )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2" )
				.hasTotalHitCount( 2 );

		predicate = scope.predicate().bool()
				.should( f -> f.match().field( "integer" ).matching( 1 ) )
				.should( f -> f.match().field( "integer" ).matching( 2 ) )
				.toPredicate();
		query = scope.query()
				.where( predicate )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2" )
				.hasTotalHitCount( 2 );
	}

	private void initData() {
		index.bulkIndexer()
				.add( "1", document -> {
					document.addValue( index.binding().string, "text 1" );
					document.addValue( index.binding().string_analyzed, "text 1" );
					document.addValue( index.binding().integer, 1 );
					document.addValue( index.binding().localDate, LocalDate.of( 2018, 1, 1 ) );
					document.addValue( index.binding().geoPoint, GeoPoint.of( 0, 1 ) );

					DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
					flattenedObject.addValue( index.binding().flattenedObject.string, "text 1_1" );
					flattenedObject.addValue( index.binding().flattenedObject.integer, 101 );
					flattenedObject = document.addObject( index.binding().flattenedObject.self );
					flattenedObject.addValue( index.binding().flattenedObject.string, "text 1_2" );
					flattenedObject.addValue( index.binding().flattenedObject.integer, 102 );

					DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
					nestedObject.addValue( index.binding().nestedObject.string, "text 1_1" );
					nestedObject.addValue( index.binding().nestedObject.integer, 101 );
					nestedObject = document.addObject( index.binding().nestedObject.self );
					nestedObject.addValue( index.binding().nestedObject.string, "text 1_2" );
					nestedObject.addValue( index.binding().nestedObject.integer, 102 );
				} )
				.add( "2", document -> {
					document.addValue( index.binding().string, "text 2" );
					document.addValue( index.binding().string_analyzed, "text 2" );
					document.addValue( index.binding().integer, 2 );
					document.addValue( index.binding().localDate, LocalDate.of( 2018, 1, 2 ) );
					document.addValue( index.binding().geoPoint, GeoPoint.of( 0, 2 ) );

					DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
					flattenedObject.addValue( index.binding().flattenedObject.string, "text 2_1" );
					flattenedObject.addValue( index.binding().flattenedObject.integer, 201 );
					flattenedObject = document.addObject( index.binding().flattenedObject.self );
					flattenedObject.addValue( index.binding().flattenedObject.string, "text 2_2" );
					flattenedObject.addValue( index.binding().flattenedObject.integer, 202 );

					DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
					nestedObject.addValue( index.binding().nestedObject.string, "text 2_1" );
					nestedObject.addValue( index.binding().nestedObject.integer, 201 );
					nestedObject = document.addObject( index.binding().nestedObject.self );
					nestedObject.addValue( index.binding().nestedObject.string, "text 2_2" );
					nestedObject.addValue( index.binding().nestedObject.integer, 202 );
				} )
				.add( "3", document -> {
					document.addValue( index.binding().string, "text 3" );
					document.addValue( index.binding().string_analyzed, "text 3" );
					document.addValue( index.binding().integer, 3 );
				} )
				.add( "neverMatching", document -> {
					document.addValue( index.binding().string, "never matching" );
					document.addValue( index.binding().string_analyzed, "never matching" );
					document.addValue( index.binding().integer, 9484 );
				} )
				.add( "empty", document -> { } )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> string_analyzed;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<LocalDate> localDate;
		final IndexFieldReference<GeoPoint> geoPoint;
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
			string_analyzed = root.field(
					"string_analyzed",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
			integer = root.field( "integer", f -> f.asInteger() ).toReference();
			localDate = root.field( "localDate", f -> f.asLocalDate() ).toReference();
			geoPoint = root.field( "geoPoint", f -> f.asGeoPoint() ).toReference();
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectStructure.FLATTENED )
					.multiValued();
			flattenedObject = new ObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectStructure.NESTED )
					.multiValued();
			nestedObject = new ObjectMapping( nestedObjectField );
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<String> string;
		final IndexFieldReference<Integer> integer;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			string = objectField.field( "string", f -> f.asString() ).toReference();
			integer = objectField.field( "integer", f -> f.asInteger() ).toReference();
		}
	}
}
