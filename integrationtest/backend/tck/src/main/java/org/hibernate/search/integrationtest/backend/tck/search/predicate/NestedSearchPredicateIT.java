/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NestedSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "nestedQueryShouldMatchId";
	private static final String DOCUMENT_2 = "nonNestedQueryShouldMatchId";

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

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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
	public void search_nestedOnTwoLevels() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.bool()
								// This is referred to as "condition 1" in the data initialization method
								.must( f.nested().onObjectField( "nestedObject.nestedObject" )
										.nest( f.bool()
												.must( f.match()
														.onField( "nestedObject.nestedObject.field1" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 )
												)
												.must( f.match()
														.onField( "nestedObject.nestedObject.field2" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 )
												)
										)
								)
								// This is referred to as "condition 2" in the data initialization method
								.must( f.nested().onObjectField( "nestedObject.nestedObject" )
										.nest( f.bool()
												.must( f.match()
														.onField( "nestedObject.nestedObject.field1" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 )
												)
												.must( f.match()
														.onField( "nestedObject.nestedObject.field2" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
												)
										)
								)
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void search_nestedOnTwoLevels_onlySecondLevel() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						// This is referred to as "condition 1" in the data initialization method
						.must( f.nested().onObjectField( "nestedObject.nestedObject" )
								.nest( f.bool()
										.must( f.match()
												.onField( "nestedObject.nestedObject.field1" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 )
										)
										.must( f.match()
												.onField( "nestedObject.nestedObject.field2" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 )
										)
								)
						)
						// This is referred to as "condition 2" in the data initialization method
						.must( f.nested().onObjectField( "nestedObject.nestedObject" )
								.nest( f.bool()
										.must( f.match()
												.onField( "nestedObject.nestedObject.field1" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 )
										)
										.must( f.match()
												.onField( "nestedObject.nestedObject.field2" )
												.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
										)
								)
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 )
				.hasTotalHitCount( 2 );
	}

	@Test
	public void search_nestedOnTwoLevels_conditionOnFirstLevel() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.bool()
								.must( f.match()
										.onField( "nestedObject.string" )
										.matching( MATCHING_STRING )
								)
								// This is referred to as "condition 2" in the data initialization method
								.must( f.nested().onObjectField( "nestedObject.nestedObject" )
										.nest( f.bool()
												.must( f.match()
														.onField( "nestedObject.nestedObject.field1" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 )
												)
												.must( f.match()
														.onField( "nestedObject.nestedObject.field2" )
														.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
												)
										)
								)
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void search_nestedOnTwoLevels_separatePredicates() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchPredicate predicate1 = scope.predicate().nested().onObjectField( "nestedObject.nestedObject" )
				.nest( f -> f.bool()
						.must( f.match()
								.onField( "nestedObject.nestedObject.field1" )
								.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 )
						).must( f.match()
								.onField( "nestedObject.nestedObject.field2" )
								.matching( MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 )
						)
				)
				.toPredicate();

		SearchPredicate predicate2 = scope.predicate().nested().onObjectField( "nestedObject.nestedObject" )
				.nest( f -> f.bool()
						.must( f.match()
								.onField( "nestedObject.nestedObject.field1" )
								.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 )
						).must( f.match()
								.onField( "nestedObject.nestedObject.field2" )
								.matching( MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 )
						)
				)
				.toPredicate();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.nested().onObjectField( "nestedObject" )
						.nest( f.bool()
								// This is referred to as "condition 1" in the data initialization method
								.must( predicate1 )
								// This is referred to as "condition 2" in the data initialization method
								.must( predicate2 )
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 )
				.hasTotalHitCount( 1 );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			ObjectMapping level1;
			SecondLevelObjectMapping level2;
			DocumentElement object;
			DocumentElement secondLevelObject;

			level1 = indexMapping.nestedObject;
			level2 = level1.nestedObject;

			object = level1.self.add( document );
			level2.self.addMissing( object );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );

			// This object will trigger the match; others should not
			object = level1.self.add( document );
			level1.string.write( object, NON_MATCHING_STRING );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
			level2.self.addMissing( object );
			secondLevelObject = level2.self.add( object ); // This matches nested condition 1
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
			secondLevelObject = level2.self.add( object ); // This matches nested condition 2
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );

			object = level1.self.add( document );
			level2.self.addMissing( object );
		} );

		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			ObjectMapping level1 = indexMapping.nestedObject;
			DocumentElement object = level1.self.add( document );
			SecondLevelObjectMapping level2 = level1.nestedObject;
			DocumentElement secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );

			object = level1.self.add( document );
			level1.string.write( object, NON_MATCHING_STRING );
			secondLevelObject = level2.self.add( object ); // This matches nested condition 1
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );

			object = level1.self.add( document );
			level1.string.write( object, MATCHING_STRING );
			level2.self.addMissing( object );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

			object = level1.self.add( document );
			level1.string.write( object, MATCHING_STRING );
			secondLevelObject = level2.self.add( object ); // This matches nested condition 2
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

			object = level1.self.add( document );
		} );

		workPlan.add( referenceProvider( "neverMatching" ), document -> {
			ObjectMapping level1 = indexMapping.nestedObject;
			SecondLevelObjectMapping level2 = level1.nestedObject;

			DocumentElement object = level1.self.add( document );
			DocumentElement secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );

			object = level1.self.add( document );
			level1.string.write( object, NON_MATCHING_STRING );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
			level2.self.addMissing( object );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

			object = level1.self.add( document );
			level1.string.write( object, NON_MATCHING_STRING );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

			object = level1.self.add( document );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD1 );
			level2.field2.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION2_FIELD2 );

			object = level1.self.add( document );
			secondLevelObject = level2.self.add( object );
			level2.field1.write( secondLevelObject, MATCHING_SECOND_LEVEL_CONDITION1_FIELD1 );
			level2.field2.write( secondLevelObject, NON_MATCHING_SECOND_LEVEL_CONDITION1_FIELD2 );
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
				.hasDocRefHitsAnyOrder(
						INDEX_NAME,
						DOCUMENT_1, DOCUMENT_2, "neverMatching", "empty"
				);
	}

	private static class IndexMapping {
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new ObjectMapping( nestedObjectField );
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
					ObjectFieldStorage.NESTED
			);
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
			field2 = objectField.field( "field2", f -> f.asString() ).toReference();
		}
	}
}
