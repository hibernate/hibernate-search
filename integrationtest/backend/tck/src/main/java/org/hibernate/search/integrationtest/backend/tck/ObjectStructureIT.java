/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.time.LocalDate;
import java.util.Arrays;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ObjectStructureIT {

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
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void index_error_invalidFieldForDocumentElement_root() {
		assertThatThrownBy( () -> index.index( "willNotWork", document -> {
			DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
			flattenedObject.addValue( index.binding().string, "willNotWork" );
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid field reference for this document element",
						"this document element has path 'null', but the referenced field has a parent with path 'flattenedObject'."
				);

	}

	@Test
	public void index_error_invalidFieldForDocumentElement_flattened() {
		assertThatThrownBy( () -> index.index( "willNotWork",
				document -> document.addValue( index.binding().flattenedObject.string, "willNotWork" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid field reference for this document element",
						"this document element has path 'flattenedObject', but the referenced field has a parent with path 'null'."
				);
	}

	@Test
	public void index_error_invalidFieldForDocumentElement_nested() {
		assertThatThrownBy( () -> index.index( "willNotWork",
				document -> document.addValue( index.binding().nestedObject.string, "willNotWork" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid field reference for this document element",
						"this document element has path 'nestedObject', but the referenced field has a parent with path 'null'."
				);
	}

	@Test
	public void search_match() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.and(
						f.match().field( "flattenedObject.string" ).matching( MATCHING_STRING ),
						f.match().field( "flattenedObject.string_analyzed" ).matching( MATCHING_STRING_ANALYZED ),
						f.match().field( "flattenedObject.integer" ).matching( MATCHING_INTEGER ),
						f.match().field( "flattenedObject.localDate" ).matching( MATCHING_LOCAL_DATE )
				) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), EXPECTED_NON_NESTED_MATCH_ID )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.where( f -> f.nested( "nestedObject" )
						.add( f.match().field( "nestedObject.string" ).matching( MATCHING_STRING ) )
						.add( f.match().field( "nestedObject.string_analyzed" ).matching( MATCHING_STRING_ANALYZED ) )
						.add( f.match().field( "nestedObject.integer" ).matching( MATCHING_INTEGER ) )
						.add( f.match().field( "nestedObject.localDate" ).matching( MATCHING_LOCAL_DATE ) )
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), EXPECTED_NESTED_MATCH_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void search_range() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.and(
						f.range().field( "flattenedObject.string" )
								.between( MATCHING_STRING, MATCHING_STRING ),
						f.range().field( "flattenedObject.integer" )
								.between( MATCHING_INTEGER - 1, MATCHING_INTEGER + 1 ),
						f.range().field( "flattenedObject.localDate" )
								.between( MATCHING_LOCAL_DATE.minusDays( 1 ), MATCHING_LOCAL_DATE.plusDays( 1 ) )
				) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), EXPECTED_NON_NESTED_MATCH_ID )
				.hasTotalHitCount( 1 );

		query = scope.query()
				.where( f -> f.nested( "nestedObject" )
						.add( f.range().field( "nestedObject.string" )
								.between( MATCHING_STRING, MATCHING_STRING )
						)
						.add( f.range().field( "nestedObject.integer" )
								.between( MATCHING_INTEGER - 1, MATCHING_INTEGER + 1 )
						)
						.add( f.range().field( "nestedObject.localDate" )
								.between( MATCHING_LOCAL_DATE.minusDays( 1 ), MATCHING_LOCAL_DATE.plusDays( 1 ) )
						)
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), EXPECTED_NESTED_MATCH_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void search_error_nonNestedField() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.predicate().nested( "flattenedObject" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot use 'predicate:nested' on field 'flattenedObject'",
						"Some object field features require a nested structure",
						"try setting the field structure to 'NESTED' and reindexing all your data" );
	}

	@Test
	public void search_error_nonObjectField() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.predicate().nested( "flattenedObject.string" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot use 'predicate:nested' on field 'flattenedObject.string'",
						"'predicate:nested' is not available for fields of this type" );
	}

	@Test
	public void search_error_missingField() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.predicate().nested( "doesNotExist" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unknown field", "'doesNotExist'" );
	}

	private void initData() {
		index.bulkIndexer()
				.add( EXPECTED_NESTED_MATCH_ID, document -> {
					ObjectMapping objectMapping;
					DocumentElement object;

					// ----------------
					// Flattened object
					// Leave it empty.
					objectMapping = index.binding().flattenedObject;
					document.addNullObject( objectMapping.self );

					// -------------
					// Nested object
					// Content specially crafted to match in nested queries.
					objectMapping = index.binding().nestedObject;

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
				} )
				.add( EXPECTED_NON_NESTED_MATCH_ID, document -> {
					/*
					 * Below, we use the same content for both the flattened object and the nested object.
					 * This is to demonstrate the practical difference of object structure:
					 * with the same content and similar conditions, the queries on the flattened object should match,
					 * but the queries on the nested object should not.
					 * In short, there is content that matches each and every condition used in tests,
					 * but this content is spread over several objects, meaning it will only match
					 * if a flattened structure is used.
					 */
					for ( ObjectMapping objectMapping : Arrays.asList( index.binding().flattenedObject,
							index.binding().nestedObject ) ) {
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
				} )
				.add( "neverMatching", document -> {
					/*
					 * This should not match, be it on the nested or the flattened object.
					 * For first-level nesting tests, it's because of the integer field.
					 * For second-level nesting tests, it's because there is no nested object matching condition 1.
					 */
					for ( ObjectMapping objectMapping : Arrays.asList( index.binding().flattenedObject,
							index.binding().nestedObject ) ) {
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
				} )
				.add( "empty", document -> {} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
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
