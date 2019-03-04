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
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
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

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void index_error_invalidDocumentElement_root() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid parent object for this field accessor" );
		thrown.expectMessage( "expected path 'null', got 'flattenedObject'." );

		workPlan.add( referenceProvider( "willNotWork" ), document -> {
			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.string.write( flattenedObject, "willNotWork" );
		} );

		workPlan.execute().join();
	}

	@Test
	public void index_error_invalidDocumentElement_flattened() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid parent object for this field accessor" );
		thrown.expectMessage( "expected path 'flattenedObject', got 'null'." );

		workPlan.add( referenceProvider( "willNotWork" ), document -> {
			indexAccessors.flattenedObject.string.write( document, "willNotWork" );
		} );

		workPlan.execute().join();
	}

	@Test
	public void index_error_invalidDocumentElement_nested() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid parent object for this field accessor" );
		thrown.expectMessage( "expected path 'nestedObject', got 'null'." );

		workPlan.add( referenceProvider( "willNotWork" ), document -> {
			indexAccessors.nestedObject.string.write( document, "willNotWork" );
		} );

		workPlan.execute().join();
	}

	@Test
	public void search_match() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "flattenedObject.string" ).matching( MATCHING_STRING ) )
						.must( f.match().onField( "flattenedObject.string_analyzed" ).matching( MATCHING_STRING_ANALYZED ) )
						.must( f.match().onField( "flattenedObject.integer" ).matching( MATCHING_INTEGER ) )
						.must( f.match().onField( "flattenedObject.localDate" ).matching( MATCHING_LOCAL_DATE ) )
				)
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EXPECTED_NON_NESTED_MATCH_ID )
				.hasHitCount( 1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.bool()
								.must( f.match().onField( "nestedObject.string" ).matching( MATCHING_STRING ) )
								.must( f.match().onField( "nestedObject.string_analyzed" ).matching( MATCHING_STRING_ANALYZED ) )
								.must( f.match().onField( "nestedObject.integer" ).matching( MATCHING_INTEGER ) )
								.must( f.match().onField( "nestedObject.localDate" ).matching( MATCHING_LOCAL_DATE ) )
						)
				)
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EXPECTED_NESTED_MATCH_ID )
				.hasHitCount( 1 );
	}

	@Test
	public void search_range() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.range().onField( "flattenedObject.string" )
								.from( MATCHING_STRING ).to( MATCHING_STRING )
						)
						.must( f.range().onField( "flattenedObject.integer" )
								.from( MATCHING_INTEGER - 1 ).to( MATCHING_INTEGER + 1 )
						)
						.must( f.range().onField( "flattenedObject.localDate" )
								.from( MATCHING_LOCAL_DATE.minusDays( 1 ) ).to( MATCHING_LOCAL_DATE.plusDays( 1 ) )
						)
				)
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EXPECTED_NON_NESTED_MATCH_ID )
				.hasHitCount( 1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.bool()
								.must( f.range().onField( "nestedObject.string" )
										.from( MATCHING_STRING ).to( MATCHING_STRING )
								)
								.must( f.range().onField( "nestedObject.integer" )
										.from( MATCHING_INTEGER - 1 ).to( MATCHING_INTEGER + 1 )
								)
								.must( f.range().onField( "nestedObject.localDate" )
										.from( MATCHING_LOCAL_DATE.minusDays( 1 ) )
										.to( MATCHING_LOCAL_DATE.plusDays( 1 ) )
								)
						)
				)
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, EXPECTED_NESTED_MATCH_ID )
				.hasHitCount( 1 );
	}

	@Test
	public void search_error_nonNestedField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "'flattenedObject'" );
		thrown.expectMessage( "is not stored as nested" );
		searchTarget.predicate().nested().onObjectField( "flattenedObject" );
	}

	@Test
	public void search_error_nonObjectField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "'flattenedObject.string'" );
		thrown.expectMessage( "is not an object field" );
		searchTarget.predicate().nested().onObjectField( "flattenedObject.string" );
	}

	@Test
	public void search_error_missingField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "'doesNotExist'" );
		searchTarget.predicate().nested().onObjectField( "doesNotExist" );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( EXPECTED_NESTED_MATCH_ID ), document -> {
			ObjectAccessors accessors;
			DocumentElement object;

			// ----------------
			// Flattened object
			// Leave it empty.
			accessors = indexAccessors.flattenedObject;
			accessors.self.addMissing( document );

			// -------------
			// Nested object
			// Content specially crafted to match in nested queries.
			accessors = indexAccessors.nestedObject;

			object = accessors.self.add( document );
			accessors.integer.write( object, NON_MATCHING_INTEGER );

			// This object will trigger the match; others should not
			object = accessors.self.add( document );
			accessors.string.write( object, MATCHING_STRING );
			accessors.string.write( object, NON_MATCHING_STRING );
			accessors.string_analyzed.write( object, MATCHING_STRING_ANALYZED );
			accessors.integer.write( object, MATCHING_INTEGER );
			accessors.localDate.write( object, MATCHING_LOCAL_DATE );

			object = accessors.self.add( document );
			accessors.localDate.write( object, NON_MATCHING_LOCAL_DATE );
		} );

		workPlan.add( referenceProvider( EXPECTED_NON_NESTED_MATCH_ID ), document -> {
			/*
			 * Below, we use the same content for both the flattened object and the nested object.
			 * This is to demonstrate the practical difference of object storage:
			 * with the same content and similar conditions, the queries on the flattened object should match,
			 * but the queries on the nested object should not.
			 * In short, there is content that matches each and every condition used in tests,
			 * but this content is spread over several objects, meaning it will only match
			 * if flattened storage is used.
			 */
			for ( ObjectAccessors accessors :
					Arrays.asList( indexAccessors.flattenedObject, indexAccessors.nestedObject ) ) {
				DocumentElement object = accessors.self.add( document );
				accessors.integer.write( object, NON_MATCHING_INTEGER );

				object = accessors.self.add( document );
				accessors.string.write( object, NON_MATCHING_STRING );
				accessors.integer.write( object, MATCHING_INTEGER );
				accessors.integer.write( object, NON_MATCHING_INTEGER );

				object = accessors.self.add( document );
				accessors.string.write( object, MATCHING_STRING );

				object = accessors.self.add( document );
				accessors.string_analyzed.write( object, MATCHING_STRING_ANALYZED );

				object = accessors.self.add( document );
				accessors.localDate.write( object, MATCHING_LOCAL_DATE );
				accessors.string_analyzed.write( object, NON_MATCHING_STRING_ANALYZED );
			}
		} );

		workPlan.add( referenceProvider( "neverMatching" ), document -> {
			/*
			 * This should not match, be it on the nested or the flattened object.
			 * For first-level nesting tests, it's because of the integer field.
			 * For second-level nesting tests, it's because there is no nested object matching condition 1.
			 */
			for ( ObjectAccessors accessors :
					Arrays.asList( indexAccessors.flattenedObject, indexAccessors.nestedObject ) ) {
				DocumentElement object = accessors.self.add( document );
				accessors.integer.write( object, NON_MATCHING_INTEGER );

				object = accessors.self.add( document );
				accessors.string.write( object, NON_MATCHING_STRING );
				accessors.integer.write( object, NON_MATCHING_INTEGER );

				object = accessors.self.add( document );
				accessors.string.write( object, MATCHING_STRING );

				object = accessors.self.add( document );
				accessors.string_analyzed.write( object, MATCHING_STRING_ANALYZED );

				object = accessors.self.add( document );
				accessors.string_analyzed.write( object, NON_MATCHING_STRING_ANALYZED );
				accessors.localDate.write( object, MATCHING_LOCAL_DATE );
			}
		} );

		workPlan.add( referenceProvider( "empty" ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder(
						INDEX_NAME,
						EXPECTED_NESTED_MATCH_ID, EXPECTED_NON_NESTED_MATCH_ID, "neverMatching", "empty"
				);
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final ObjectAccessors flattenedObject;
		final ObjectAccessors nestedObject;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).createAccessor();
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new ObjectAccessors( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new ObjectAccessors( nestedObjectField );
		}
	}

	private static class ObjectAccessors {
		final IndexObjectFieldAccessor self;
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<String> string_analyzed;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<LocalDate> localDate;

		ObjectAccessors(IndexSchemaObjectField objectField) {
			self = objectField.createAccessor();
			string = objectField.field( "string", f -> f.asString() ).createAccessor();
			string_analyzed = objectField.field(
					"string_analyzed",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
			)
					.createAccessor();
			integer = objectField.field( "integer", f -> f.asInteger() ).createAccessor();
			localDate = objectField.field( "localDate", f -> f.asLocalDate() ).createAccessor();
		}
	}
}
