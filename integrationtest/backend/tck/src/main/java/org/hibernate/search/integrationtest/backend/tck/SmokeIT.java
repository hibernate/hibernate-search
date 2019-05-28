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
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

/**
 * @author Yoann Rodiere
 */
public class SmokeIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void predicate_match() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.match().onField( "string" ).matching( "text 1" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.predicate( f -> f.match().onField( "string_analyzed" ).matching( "text" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "3" )
				.hasTotalHitCount( 3 );

		query = scope.query()
				.predicate( f -> f.match().onField( "integer" ).matching( 1 ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.predicate( f -> f.match().onField( "localDate" ).matching( LocalDate.of( 2018, 1, 1 ) ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.predicate( f -> f.match().onField( "flattenedObject.string" ).matching( "text 1_1" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_range() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.range().onField( "string" ).from( "text 2" ).to( "text 42" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.predicate( f -> f.range().onField( "string_analyzed" ).from( "2" ).to( "42" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.predicate( f -> f.range().onField( "integer" ).from( 2 ).to( 42 ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.predicate( f -> f.range().onField( "localDate" )
						.from( LocalDate.of( 2018, 1, 2 ) )
						.to( LocalDate.of( 2018, 2, 23 ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.predicate( f -> f.range().onField( "flattenedObject.integer" ).from( 201 ).to( 242 ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2" )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_boolean() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.should( f.match().onField( "integer" ).matching( 1 ) )
						.should( f.match().onField( "integer" ).matching( 2 ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.predicate( f -> f.bool()
						.must( f.match().onField( "string_analyzed" ).matching( "text" ) )
						.filter( f.match().onField( "integer" ).matching( 1 ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.predicate( f -> f.bool()
						.must( f.match().onField( "string_analyzed" ).matching( "text" ) )
						.mustNot( f.match().onField( "integer" ).matching( 2 ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "3" )
				.hasTotalHitCount( 2 );
	}

	@Test
	public void predicate_nested() {
		StubMappingScope scope = indexManager.createScope();

		// Without nested storage, we expect predicates to be able to match on different objects
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.must( f.match().onField( "flattenedObject.string" ).matching( "text 1_2" ) )
						.must( f.match().onField( "flattenedObject.integer" ).matching( 101 ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		// With nested storage, we expect direct queries to never match
		query = scope.query()
				.predicate( f -> f.match().onField( "nestedObject.integer" ).matching( 101 ) )
				.toQuery();
		assertThat( query )
				.hasNoHits()
				.hasTotalHitCount( 0 );

		// ... and predicates within nested queries to be unable to match on different objects
		query = scope.query()
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.bool()
								.must( f.match().onField( "nestedObject.string" ).matching( "text 1_2" ) )
								.must( f.match().onField( "nestedObject.integer" ).matching( 101 ) )
						)
				)
				.toQuery();
		assertThat( query )
				.hasNoHits()
				.hasTotalHitCount( 0 );

		// ... but predicates should still be able to match on the same object
		query = scope.query()
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.bool()
								.must( f.match().onField( "nestedObject.string" ).matching( "text 1_1" ) )
								.must( f.match().onField( "nestedObject.integer" ).matching( 101 ) )
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void separatePredicate() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate = scope.predicate().match().onField( "string" ).matching( "text 1" ).toPredicate();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( predicate )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		predicate = scope.predicate().range().onField( "integer" ).from( 1 ).to( 2 ).toPredicate();
		query = scope.query()
				.predicate( predicate )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" )
				.hasTotalHitCount( 2 );

		predicate = scope.predicate().bool()
				.should( f -> f.match().onField( "integer" ).matching( 1 ) )
				.should( f -> f.match().onField( "integer" ).matching( 2 ) )
				.toPredicate();
		query = scope.query()
				.predicate( predicate )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" )
				.hasTotalHitCount( 2 );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), document -> {
			document.addValue( indexMapping.string, "text 1" );
			document.addValue( indexMapping.string_analyzed, "text 1" );
			document.addValue( indexMapping.integer, 1 );
			document.addValue( indexMapping.localDate, LocalDate.of( 2018, 1, 1 ) );
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 0, 1 ) );

			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "text 1_1" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 101 );
			flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "text 1_2" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 102 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "text 1_1" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 101 );
			nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "text 1_2" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 102 );
		} );

		workPlan.add( referenceProvider( "2" ), document -> {
			document.addValue( indexMapping.string, "text 2" );
			document.addValue( indexMapping.string_analyzed, "text 2" );
			document.addValue( indexMapping.integer, 2 );
			document.addValue( indexMapping.localDate, LocalDate.of( 2018, 1, 2 ) );
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 0, 2 ) );

			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "text 2_1" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 201 );
			flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "text 2_2" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 202 );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "text 2_1" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 201 );
			nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "text 2_2" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 202 );
		} );

		workPlan.add( referenceProvider( "3" ), document -> {
			document.addValue( indexMapping.string, "text 3" );
			document.addValue( indexMapping.string_analyzed, "text 3" );
			document.addValue( indexMapping.integer, 3 );
		} );

		workPlan.add( referenceProvider( "neverMatching" ), document -> {
			document.addValue( indexMapping.string, "never matching" );
			document.addValue( indexMapping.string_analyzed, "never matching" );
			document.addValue( indexMapping.integer, 9484 );
		} );

		workPlan.add( referenceProvider( "empty" ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "3", "neverMatching", "empty" );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> string_analyzed;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<LocalDate> localDate;
		final IndexFieldReference<GeoPoint> geoPoint;
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
			string_analyzed = root.field(
					"string_analyzed",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
			integer = root.field( "integer", f -> f.asInteger() ).toReference();
			localDate = root.field( "localDate", f -> f.asLocalDate() ).toReference();
			geoPoint = root.field( "geoPoint", f -> f.asGeoPoint() ).toReference();
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED )
					.multiValued();
			flattenedObject = new ObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectFieldStorage.NESTED )
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
