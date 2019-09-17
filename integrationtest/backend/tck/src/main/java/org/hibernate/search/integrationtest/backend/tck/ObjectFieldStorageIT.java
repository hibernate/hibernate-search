/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.Arrays;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.util.common.SearchException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ObjectFieldStorageIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String EXPECTED_NESTED_MATCH_ID = "nestedQueryShouldMatchId";
	private static final String EXPECTED_NON_NESTED_MATCH_ID = "nonNestedQueryShouldMatchId";

	private static final String MATCHING_STRING = "matchingWord";
	private static final String MATCHING_STRING_ANALYZED = "analyzedMatchingWord otherAnalyzedMatchingWord";
	private static final Integer MATCHING_INTEGER = 42;
	private static final LocalDate MATCHING_LOCAL_DATE = LocalDate.of( 2018, 2, 1 );

	private static final String NON_MATCHING_STRING = "nonMatchingWord";
	private static final String NON_MATCHING_STRING_ANALYZED = "analyzedNonMatchingWord otherAnalyzedNonMatchingWord";
	private static final Integer NON_MATCHING_INTEGER = 442;
	private static final LocalDate NON_MATCHING_LOCAL_DATE = LocalDate.of( 2018, 2, 15 );

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void index_error_invalidFieldForDocumentElement_root() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid field reference for this document element" );
		thrown.expectMessage( "this document element has path 'null', but the referenced field has a parent with path 'flattenedObject'." );

		plan.add( referenceProvider( "willNotWork" ), document -> {
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.string, "willNotWork" );
		} );

		plan.execute().join();
	}

	@Test
	public void index_error_invalidFieldForDocumentElement_flattened() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid field reference for this document element" );
		thrown.expectMessage( "this document element has path 'flattenedObject', but the referenced field has a parent with path 'null'." );

		plan.add( referenceProvider( "willNotWork" ), document -> {
			document.addValue( indexMapping.flattenedObject.string, "willNotWork" );
		} );

		plan.execute().join();
	}

	@Test
	public void index_error_invalidFieldForDocumentElement_nested() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid field reference for this document element" );
		thrown.expectMessage( "this document element has path 'nestedObject', but the referenced field has a parent with path 'null'." );

		plan.add( referenceProvider( "willNotWork" ), document -> {
			document.addValue( indexMapping.nestedObject.string, "willNotWork" );
		} );

		plan.execute().join();
	}

	@Test
	public void search_match() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.must( f.match().field( "flattenedObject.string" ).matching( MATCHING_STRING ) )
						.must( f.match().field( "flattenedObject.string_analyzed" ).matching( MATCHING_STRING_ANALYZED ) )
						.must( f.match().field( "flattenedObject.integer" ).matching( MATCHING_INTEGER ) )
						.must( f.match().field( "flattenedObject.localDate" ).matching( MATCHING_LOCAL_DATE ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EXPECTED_NON_NESTED_MATCH_ID )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.predicate( f -> f.nested().objectField( "nestedObject" )
						.nest( f.bool()
								.must( f.match().field( "nestedObject.string" ).matching( MATCHING_STRING ) )
								.must( f.match().field( "nestedObject.string_analyzed" ).matching( MATCHING_STRING_ANALYZED ) )
								.must( f.match().field( "nestedObject.integer" ).matching( MATCHING_INTEGER ) )
								.must( f.match().field( "nestedObject.localDate" ).matching( MATCHING_LOCAL_DATE ) )
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EXPECTED_NESTED_MATCH_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void search_range() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.must( f.range().field( "flattenedObject.string" )
								.from( MATCHING_STRING ).to( MATCHING_STRING )
						)
						.must( f.range().field( "flattenedObject.integer" )
								.from( MATCHING_INTEGER - 1 ).to( MATCHING_INTEGER + 1 )
						)
						.must( f.range().field( "flattenedObject.localDate" )
								.from( MATCHING_LOCAL_DATE.minusDays( 1 ) ).to( MATCHING_LOCAL_DATE.plusDays( 1 ) )
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EXPECTED_NON_NESTED_MATCH_ID )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.predicate( f -> f.nested().objectField( "nestedObject" )
						.nest( f.bool()
								.must( f.range().field( "nestedObject.string" )
										.from( MATCHING_STRING ).to( MATCHING_STRING )
								)
								.must( f.range().field( "nestedObject.integer" )
										.from( MATCHING_INTEGER - 1 ).to( MATCHING_INTEGER + 1 )
								)
								.must( f.range().field( "nestedObject.localDate" )
										.from( MATCHING_LOCAL_DATE.minusDays( 1 ) )
										.to( MATCHING_LOCAL_DATE.plusDays( 1 ) )
								)
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EXPECTED_NESTED_MATCH_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void search_error_nonNestedField() {
		StubMappingScope scope = indexManager.createScope();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "'flattenedObject'" );
		thrown.expectMessage( "is not stored as nested" );
		scope.predicate().nested().objectField( "flattenedObject" );
	}

	@Test
	public void search_error_nonObjectField() {
		StubMappingScope scope = indexManager.createScope();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "'flattenedObject.string'" );
		thrown.expectMessage( "is not an object field" );
		scope.predicate().nested().objectField( "flattenedObject.string" );
	}

	@Test
	public void search_error_missingField() {
		StubMappingScope scope = indexManager.createScope();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "'doesNotExist'" );
		scope.predicate().nested().objectField( "doesNotExist" );
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( EXPECTED_NESTED_MATCH_ID ), document -> {
			ObjectMapping objectMapping;
			DocumentElement object;

			// ----------------
			// Flattened object
			// Leave it empty.
			objectMapping = indexMapping.flattenedObject;
			document.addNullObject( objectMapping.self );

			// -------------
			// Nested object
			// Content specially crafted to match in nested queries.
			objectMapping = indexMapping.nestedObject;

			object = document.addObject( objectMapping.self );
			object.addValue( objectMapping.integer, NON_MATCHING_INTEGER );

			// This object will trigger the match; others should not
			object = document.addObject( objectMapping.self );
			object.addValue( objectMapping.string, MATCHING_STRING );
			object.addValue( objectMapping.string, NON_MATCHING_STRING );
			object.addValue( objectMapping.string_analyzed, MATCHING_STRING_ANALYZED );
			object.addValue( objectMapping.integer, MATCHING_INTEGER );
			object.addValue( objectMapping.localDate, MATCHING_LOCAL_DATE );

			object = document.addObject( objectMapping.self );
			object.addValue( objectMapping.localDate, NON_MATCHING_LOCAL_DATE );
		} );

		plan.add( referenceProvider( EXPECTED_NON_NESTED_MATCH_ID ), document -> {
			/*
			 * Below, we use the same content for both the flattened object and the nested object.
			 * This is to demonstrate the practical difference of object storage:
			 * with the same content and similar conditions, the queries on the flattened object should match,
			 * but the queries on the nested object should not.
			 * In short, there is content that matches each and every condition used in tests,
			 * but this content is spread over several objects, meaning it will only match
			 * if flattened storage is used.
			 */
			for ( ObjectMapping objectMapping :
					Arrays.asList( indexMapping.flattenedObject, indexMapping.nestedObject ) ) {
				DocumentElement object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.integer, NON_MATCHING_INTEGER );

				object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.string, NON_MATCHING_STRING );
				object.addValue( objectMapping.integer, MATCHING_INTEGER );
				object.addValue( objectMapping.integer, NON_MATCHING_INTEGER );

				object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.string, MATCHING_STRING );

				object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.string_analyzed, MATCHING_STRING_ANALYZED );

				object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.localDate, MATCHING_LOCAL_DATE );
				object.addValue( objectMapping.string_analyzed, NON_MATCHING_STRING_ANALYZED );
			}
		} );

		plan.add( referenceProvider( "neverMatching" ), document -> {
			/*
			 * This should not match, be it on the nested or the flattened object.
			 * For first-level nesting tests, it's because of the integer field.
			 * For second-level nesting tests, it's because there is no nested object matching condition 1.
			 */
			for ( ObjectMapping objectMapping :
					Arrays.asList( indexMapping.flattenedObject, indexMapping.nestedObject ) ) {
				DocumentElement object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.integer, NON_MATCHING_INTEGER );

				object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.string, NON_MATCHING_STRING );
				object.addValue( objectMapping.integer, NON_MATCHING_INTEGER );

				object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.string, MATCHING_STRING );

				object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.string_analyzed, MATCHING_STRING_ANALYZED );

				object = document.addObject( objectMapping.self );
				object.addValue( objectMapping.string_analyzed, NON_MATCHING_STRING_ANALYZED );
				object.addValue( objectMapping.localDate, MATCHING_LOCAL_DATE );
			}
		} );

		plan.add( referenceProvider( "empty" ), document -> { } );

		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder(
						INDEX_NAME,
						EXPECTED_NESTED_MATCH_ID, EXPECTED_NON_NESTED_MATCH_ID, "neverMatching", "empty"
				);
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
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
		final IndexFieldReference<String> string_analyzed;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<LocalDate> localDate;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			string = objectField.field( "string", f -> f.asString() )
					.multiValued()
					.toReference();
			string_analyzed = objectField.field(
					"string_analyzed",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
			integer = objectField.field( "integer", f -> f.asInteger() )
					.multiValued()
					.toReference();
			localDate = objectField.field( "localDate", f -> f.asLocalDate() ).toReference();
		}
	}
}
