/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests for the legacy syntax of nested predicate definition.
 */
@SuppressWarnings("deprecation")
public class NestedPredicateLegacyIT {

	private static final String DOCUMENT_1 = "nestedQueryShouldMatchId";
	private static final String DOCUMENT_2 = "nonNestedQueryShouldMatchId";

	private static final String MISSING_FIELD_INDEX_DOCUMENT_1 = "missingFieldIndex_1";

	private static final String MATCHING_STRING = "matchingWord";
	private static final String MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 = "firstMatchingWord";
	private static final String MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 = "firstMatchingWord";
	private static final String MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 = "secondMatchingWord";
	private static final String MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 = "secondMatchingWord";

	private static final String NON_MATCHING_STRING = "nonMatchingWord";
	private static final String NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 = "firstNonMatchingWord";
	private static final String NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 = "firstNonMatchingWord";
	private static final String NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 = "secondNonMatchingWord";
	private static final String NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 = "secondNonMatchingWord";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new );
	private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
			SimpleMappedIndex.of( MissingFieldIndexBinding::new ).name( "missingField" );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( mainIndex, missingFieldIndex ).setup();

		initData();
	}

	@Test
	public void search_nestedOnTwoLevels() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.nested().objectField( "nestedObject" )
						.nest( f.and(
								// This is referred to as "condition 1" in the data initialization method
								f.nested().objectField( "nestedObject.nestedObject" )
										.nest( f.and(
												f.match()
														.field( "nestedObject.nestedObject.field1" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 ),
												f.match()
														.field( "nestedObject.nestedObject.field2" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 )
										) ),
								// This is referred to as "condition 2" in the data initialization method
								f.nested().objectField( "nestedObject.nestedObject" )
										.nest( f.and(
												f.match()
														.field( "nestedObject.nestedObject.field1" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 ),
												f.match()
														.field( "nestedObject.nestedObject.field2" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
										) )
						) )
				) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void search_nestedOnTwoLevels_onlySecondLevel() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.and(
						// This is referred to as "condition 1" in the data initialization method
						f.nested().objectField( "nestedObject.nestedObject" )
								.nest( f.and(
										f.match()
												.field( "nestedObject.nestedObject.field1" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 ),
										f.match()
												.field( "nestedObject.nestedObject.field2" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 )
								) ),
						// This is referred to as "condition 2" in the data initialization method
						f.nested().objectField( "nestedObject.nestedObject" )
								.nest( f.and(
										f.match()
												.field( "nestedObject.nestedObject.field1" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 ),
										f.match()
												.field( "nestedObject.nestedObject.field2" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
								) )
				) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 )
				.hasTotalHitCount( 2 );
	}

	@Test
	public void search_nestedOnTwoLevels_conditionOnFirstLevel() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.nested().objectField( "nestedObject" )
						.nest( f.and(
								f.match()
										.field( "nestedObject.string" )
										.matching( MATCHING_STRING ),
								// This is referred to as "condition 2" in the data initialization method
								f.nested().objectField( "nestedObject.nestedObject" )
										.nest( f.and(
												f.match()
														.field( "nestedObject.nestedObject.field1" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 ),
												f.match()
														.field( "nestedObject.nestedObject.field2" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
										) )
						) )
				) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void search_nestedOnTwoLevels_separatePredicates() {
		StubMappingScope scope = mainIndex.createScope();

		SearchPredicate predicate1 = scope.predicate().nested().objectField( "nestedObject.nestedObject" )
				.nest( f -> f.and(
						f.match()
								.field( "nestedObject.nestedObject.field1" )
								.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 ),
						f.match()
								.field( "nestedObject.nestedObject.field2" )
								.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 )
				) )
				.toPredicate();

		SearchPredicate predicate2 = scope.predicate().nested().objectField( "nestedObject.nestedObject" )
				.nest( f -> f.and(
						f.match()
								.field( "nestedObject.nestedObject.field1" )
								.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 ),
						f.match()
								.field( "nestedObject.nestedObject.field2" )
								.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
				) )
				.toPredicate();

		assertThatQuery( scope.query()
				.where( f -> f.nested().objectField( "nestedObject" )
						.nest( f.bool()
								// This is referred to as "condition 1" in the data initialization method
								.must( predicate1 )
								// This is referred to as "condition 2" in the data initialization method
								.must( predicate2 )
						)
				) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void invalidNestedPath_parent() {
		String objectFieldPath = "nestedObject";
		String fieldInParentPath = "string";

		assertThatThrownBy( () -> mainIndex.query()
				.where( f -> f.nested().objectField( objectFieldPath )
						.nest( f.and(
								f.match()
										.field( fieldInParentPath )
										.matching( "irrelevant_because_this_will_fail" ),
								f.match()
										.field( fieldInParentPath )
										.matching( "irrelevant_because_this_will_fail" )
						) )
				) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search predicate",
						"This predicate targets fields [" + fieldInParentPath + "]",
						"only fields that are contained in the nested object with path '"
								+ objectFieldPath + "'"
								+ " are allowed here." );
	}

	@Test
	public void invalidNestedPath_sibling() {
		String objectFieldPath = "nestedObject";
		String fieldInSiblingPath = "nestedObject2.string";

		assertThatThrownBy( () -> mainIndex.query()
				.where( f -> f.nested().objectField( objectFieldPath )
						.nest( f.and(
								f.match()
										.field( fieldInSiblingPath )
										.matching( "irrelevant_because_this_will_fail" ),
								f.match()
										.field( fieldInSiblingPath )
										.matching( "irrelevant_because_this_will_fail" )
						) )
				) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search predicate",
						"This predicate targets fields [" + fieldInSiblingPath + "]",
						"only fields that are contained in the nested object with path '"
								+ objectFieldPath + "'"
								+ " are allowed here." );
	}

	/**
	 * Test that no failure occurs when a nested predicate targets a nested field
	 * that only exists in one of the targeted indexes.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_missingNestedField() {
		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );
		SearchPredicateFactory f = scope.predicate();
		SearchPredicate nestedPredicate = f.nested().objectField( "nestedObject" )
				.nest( f.and(
						// This is referred to as "condition 1" in the data initialization method
						f.nested().objectField( "nestedObject.nestedObject" )
								.nest( f.and(
										f.match()
												.field( "nestedObject.nestedObject.field1" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 ),
										f.match()
												.field( "nestedObject.nestedObject.field2" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 )
								) ),
						// This is referred to as "condition 2" in the data initialization method
						f.nested().objectField( "nestedObject.nestedObject" )
								.nest( f.and(
										f.match()
												.field( "nestedObject.nestedObject.field1" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 ),
										f.match()
												.field( "nestedObject.nestedObject.field2" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
								) )
				) )
				.toPredicate();

		// The "nested" predicate should not match anything in missingFieldIndex
		assertThatQuery( mainIndex.createScope( missingFieldIndex ).query()
				.where( nestedPredicate ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 )
				.hasTotalHitCount( 1 );

		// ... but it should not prevent the query from executing either:
		// if the "nested" predicate is optional, it should be ignored for missingFieldIndex.
		assertThatQuery( mainIndex.createScope( missingFieldIndex ).query()
				.where( f.bool()
						.should( nestedPredicate )
						.should( f.id().matching( MISSING_FIELD_INDEX_DOCUMENT_1 ) )
						.toPredicate() ) )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( mainIndex.typeName(), DOCUMENT_1 )
						.doc( missingFieldIndex.typeName(), MISSING_FIELD_INDEX_DOCUMENT_1 ) )
				.hasTotalHitCount( 2 );
	}

	private static void initData() {
		BulkIndexer mainIndexIndexer = mainIndex.bulkIndexer();
		mainIndexIndexer
				.add( DOCUMENT_1, document -> {
					ObjectMapping level1;
					SecondLevelObjectMapping level2;
					DocumentElement object;
					DocumentElement secondLevelObject;

					level1 = mainIndex.binding().nestedObject;
					level2 = level1.nestedObject;

					object = document.addObject( level1.self );
					object.addNullObject( level2.self );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );

					// This object will trigger the match; others should not
					object = document.addObject( level1.self );
					object.addValue( level1.string, NON_MATCHING_STRING );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
					object.addNullObject( level2.self );
					secondLevelObject = object.addObject( level2.self ); // This matches nested condition 1
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
					secondLevelObject = object.addObject( level2.self ); // This matches nested condition 2
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );

					object = document.addObject( level1.self );
					object.addNullObject( level2.self );
				} )
				.add( DOCUMENT_2, document -> {
					ObjectMapping level1 = mainIndex.binding().nestedObject;
					DocumentElement object = document.addObject( level1.self );
					SecondLevelObjectMapping level2 = level1.nestedObject;
					DocumentElement secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );

					object = document.addObject( level1.self );
					object.addValue( level1.string, NON_MATCHING_STRING );
					secondLevelObject = object.addObject( level2.self ); // This matches nested condition 1
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );

					object = document.addObject( level1.self );
					object.addValue( level1.string, MATCHING_STRING );
					object.addNullObject( level2.self );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

					object = document.addObject( level1.self );
					object.addValue( level1.string, MATCHING_STRING );
					secondLevelObject = object.addObject( level2.self ); // This matches nested condition 2
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

					object = document.addObject( level1.self );
				} )
				.add( "neverMatching", document -> {
					ObjectMapping level1 = mainIndex.binding().nestedObject;
					SecondLevelObjectMapping level2 = level1.nestedObject;

					DocumentElement object = document.addObject( level1.self );
					DocumentElement secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );

					object = document.addObject( level1.self );
					object.addValue( level1.string, NON_MATCHING_STRING );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
					object.addNullObject( level2.self );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

					object = document.addObject( level1.self );
					object.addValue( level1.string, NON_MATCHING_STRING );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

					object = document.addObject( level1.self );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
					secondLevelObject.addValue( level2.field2, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

					object = document.addObject( level1.self );
					secondLevelObject = object.addObject( level2.self );
					secondLevelObject.addValue( level2.field1, MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
					secondLevelObject.addValue( level2.field2, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
				} )
				.add( "empty", document -> {} );

		BulkIndexer missingFieldIndexIndexer = missingFieldIndex.bulkIndexer();
		missingFieldIndexIndexer.add( MISSING_FIELD_INDEX_DOCUMENT_1, document -> {} );

		mainIndexIndexer.join( missingFieldIndexIndexer );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final ObjectMapping nestedObject;
		final ObjectMapping nestedObject2;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();

			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectStructure.NESTED )
					.multiValued();
			nestedObject = new ObjectMapping( nestedObjectField );
			IndexSchemaObjectField nestedObject2Field = root.objectField( "nestedObject2", ObjectStructure.NESTED )
					.multiValued();
			nestedObject2 = new ObjectMapping( nestedObject2Field );
		}
	}

	private static class MissingFieldIndexBinding {
		MissingFieldIndexBinding(IndexSchemaElement root) {
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<String> string;
		final SecondLevelObjectMapping nestedObject;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			string = objectField.field( "string", f -> f.asString() ).toReference();
			IndexSchemaObjectField nestedObjectField = objectField.objectField(
					"nestedObject",
					ObjectStructure.NESTED
			)
					.multiValued();
			nestedObject = new SecondLevelObjectMapping( nestedObjectField );
		}
	}

	private static class SecondLevelObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<String> field1;
		final IndexFieldReference<String> field2;

		SecondLevelObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			field1 = objectField.field( "field1", f -> f.asString() ).toReference();
			field2 = objectField.field( "field2", f -> f.asString() ).multiValued().toReference();
		}
	}
}
