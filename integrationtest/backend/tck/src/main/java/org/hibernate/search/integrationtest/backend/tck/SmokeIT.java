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
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
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
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.match().onField( "string" ).matching( "text 1" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.match().onField( "string_analyzed" ).matching( "text" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "3" )
				.hasTotalHitCount( 3 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.match().onField( "integer" ).matching( 1 ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.match().onField( "localDate" ).matching( LocalDate.of( 2018, 1, 1 ) ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.match().onField( "flattenedObject.string" ).matching( "text 1_1" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_range() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.range().onField( "string" ).from( "text 2" ).to( "text 42" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.range().onField( "string_analyzed" ).from( "2" ).to( "42" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.range().onField( "integer" ).from( 2 ).to( 42 ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2", "3" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.range().onField( "localDate" )
						.from( LocalDate.of( 2018, 1, 2 ) )
						.to( LocalDate.of( 2018, 2, 23 ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.range().onField( "flattenedObject.integer" ).from( 201 ).to( 242 ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2" )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_boolean() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.match().onField( "integer" ).matching( 1 ) )
						.should( f.match().onField( "integer" ).matching( 2 ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" )
				.hasTotalHitCount( 2 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "string_analyzed" ).matching( "text" ) )
						.filter( f.match().onField( "integer" ).matching( 1 ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.asReference()
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
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// Without nested storage, we expect predicates to be able to match on different objects
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
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
				.asReference()
				.predicate( f -> f.match().onField( "nestedObject.integer" ).matching( 101 ) )
				.toQuery();
		assertThat( query )
				.hasNoHits()
				.hasTotalHitCount( 0 );

		// ... and predicates within nested queries to be unable to match on different objects
		query = scope.query()
				.asReference()
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
				.asReference()
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
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchPredicate predicate = scope.predicate().match().onField( "string" ).matching( "text 1" ).toPredicate();
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( predicate )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" )
				.hasTotalHitCount( 1 );

		predicate = scope.predicate().range().onField( "integer" ).from( 1 ).to( 2 ).toPredicate();
		query = scope.query()
				.asReference()
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
				.asReference()
				.predicate( predicate )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" )
				.hasTotalHitCount( 2 );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), document -> {
			indexMapping.string.write( document, "text 1" );
			indexMapping.string_analyzed.write( document, "text 1" );
			indexMapping.integer.write( document, 1 );
			indexMapping.localDate.write( document, LocalDate.of( 2018, 1, 1 ) );
			indexMapping.geoPoint.write( document, GeoPoint.of( 0, 1 ) );

			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.string.write( flattenedObject, "text 1_1" );
			indexMapping.flattenedObject.integer.write( flattenedObject, 101 );
			flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.string.write( flattenedObject, "text 1_2" );
			indexMapping.flattenedObject.integer.write( flattenedObject, 102 );

			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.string.write( nestedObject, "text 1_1" );
			indexMapping.nestedObject.integer.write( nestedObject, 101 );
			nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.string.write( nestedObject, "text 1_2" );
			indexMapping.nestedObject.integer.write( nestedObject, 102 );
		} );

		workPlan.add( referenceProvider( "2" ), document -> {
			indexMapping.string.write( document, "text 2" );
			indexMapping.string_analyzed.write( document, "text 2" );
			indexMapping.integer.write( document, 2 );
			indexMapping.localDate.write( document, LocalDate.of( 2018, 1, 2 ) );
			indexMapping.geoPoint.write( document, GeoPoint.of( 0, 2 ) );

			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.string.write( flattenedObject, "text 2_1" );
			indexMapping.flattenedObject.integer.write( flattenedObject, 201 );
			flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.string.write( flattenedObject, "text 2_2" );
			indexMapping.flattenedObject.integer.write( flattenedObject, 202 );

			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.string.write( nestedObject, "text 2_1" );
			indexMapping.nestedObject.integer.write( nestedObject, 201 );
			nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.string.write( nestedObject, "text 2_2" );
			indexMapping.nestedObject.integer.write( nestedObject, 202 );
		} );

		workPlan.add( referenceProvider( "3" ), document -> {
			indexMapping.string.write( document, "text 3" );
			indexMapping.string_analyzed.write( document, "text 3" );
			indexMapping.integer.write( document, 3 );
		} );

		workPlan.add( referenceProvider( "neverMatching" ), document -> {
			indexMapping.string.write( document, "never matching" );
			indexMapping.string_analyzed.write( document, "never matching" );
			indexMapping.integer.write( document, 9484 );
		} );

		workPlan.add( referenceProvider( "empty" ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
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
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
			)
					.toReference();
			integer = root.field( "integer", f -> f.asInteger() ).toReference();
			localDate = root.field( "localDate", f -> f.asLocalDate() ).toReference();
			geoPoint = root.field( "geoPoint", f -> f.asGeoPoint() ).toReference();
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new ObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
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
