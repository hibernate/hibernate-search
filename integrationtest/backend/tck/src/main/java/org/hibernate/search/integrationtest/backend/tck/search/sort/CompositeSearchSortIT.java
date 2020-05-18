/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class CompositeSearchSortIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void byComposite() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( f -> f.composite( c -> {
			c.add( f.field( index.binding().identicalForFirstTwo.relativeFieldName ).asc() );
			c.add( f.field( index.binding().identicalForLastTwo.relativeFieldName ).asc() );
		} ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery( f -> f.composite( c -> {
			c.add( f.field( index.binding().identicalForFirstTwo.relativeFieldName ).desc() );
			c.add( f.field( index.binding().identicalForLastTwo.relativeFieldName ).desc() );
		} ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( f -> f.composite( c -> {
			c.add( f.field( index.binding().identicalForFirstTwo.relativeFieldName ).asc() );
			c.add( f.field( index.binding().identicalForLastTwo.relativeFieldName ).desc() );
		} ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery( f -> f.composite( c -> {
			c.add( f.field( index.binding().identicalForFirstTwo.relativeFieldName ).desc() );
			c.add( f.field( index.binding().identicalForLastTwo.relativeFieldName ).asc() );
		} ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void byComposite_separateSort() {
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query;

		query = simpleQuery(
				scope,
				scope.sort().composite()
						.add( scope.sort().field( index.binding().identicalForFirstTwo.relativeFieldName ).asc() )
						.add( scope.sort().field( index.binding().identicalForLastTwo.relativeFieldName ).asc() )
						.toSort()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery(
				scope,
				scope.sort().composite()
						.add( scope.sort().field( index.binding().identicalForFirstTwo.relativeFieldName ).desc() )
						.add( scope.sort().field( index.binding().identicalForLastTwo.relativeFieldName ).desc() )
						.toSort()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery(
				scope,
				scope.sort().composite()
						.add( scope.sort().field( index.binding().identicalForFirstTwo.relativeFieldName ).asc() )
						.add( scope.sort().field( index.binding().identicalForLastTwo.relativeFieldName ).desc() )
						.toSort()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery(
				scope,
				scope.sort().composite()
						.add( scope.sort().field( index.binding().identicalForFirstTwo.relativeFieldName ).desc() )
						.add( scope.sort().field( index.binding().identicalForLastTwo.relativeFieldName ).asc() )
						.toSort()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void byComposite_empty() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( f -> f.composite() );

		// Just check that the query is executed (the empty sort is ignored and nothing fails)
		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void then() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( f -> f
				.field( index.binding().identicalForFirstTwo.relativeFieldName ).asc()
				.then().field( index.binding().identicalForLastTwo.relativeFieldName ).asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery( f -> f
				.field( index.binding().identicalForFirstTwo.relativeFieldName ).desc()
				.then().field( index.binding().identicalForLastTwo.relativeFieldName ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( f -> f
				.field( index.binding().identicalForFirstTwo.relativeFieldName ).asc()
				.then().field( index.binding().identicalForLastTwo.relativeFieldName ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery( f -> f
				.field( index.binding().identicalForFirstTwo.relativeFieldName ).desc()
				.then().field( index.binding().identicalForLastTwo.relativeFieldName ).asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void then_flattened_nested() {
		SearchQuery<DocumentReference> query;

		String normalField = index.binding().identicalForFirstTwo.relativeFieldName;
		String flattenedField = "flattened." + index.binding().flattenedField.relativeFieldName;
		String nestedField = "nested." + index.binding().flattenedField.relativeFieldName;

		query = simpleQuery( f -> f.field( flattenedField ).asc().then().field( normalField ).asc() );
		// [a b a][a a b] => {1 3 2}
		assertThat( query ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3, DOCUMENT_2 );

		query = simpleQuery( f -> f.field( nestedField ).asc().then().field( flattenedField ).asc() );
		// [b a a][a b a] => {3 2 1}
		assertThat( query ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( f -> f.field( normalField ).asc().then().field( nestedField ).asc() );
		// [a a b][b a a] => {2 1 3}
		assertThat( query ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2254")
	public void then_nested_normal_limit1() {
		SearchQuery<DocumentReference> query;

		String normalField = index.binding().identicalForLastTwo.relativeFieldName;
		String nestedField = "nested." + index.binding().nestedField.relativeFieldName;

		/*
		 * This used to trigger a bug caused by the fact we were doing a two-pass sort,
		 * first sorting without the nested documents and listing the top N documents (where N is the limit),
		 * then collecting the values of the nested fields for these top N documents,
		 * then sorting all documents again with the knowledge of the values of nested fields for the top N documents.
		 * The test below would fail if, by chance, the top N documents of the first pass did not include
		 * the expected top document overall (i.e. document 2 or 3).
		 */
		query = simpleQuery( f -> f.field( nestedField ).asc().missing().last()
				.then().field( normalField ).asc() );
		// [b a a][a b b] => {[2 or 3] (3 or 2) 1}
		assertThat( query.fetch( 1 ) ).hits().ordinal( 0 )
				.isDocRefHit( index.typeName(), DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void then_flattened_nested_limit2() {
		SearchQuery<DocumentReference> query;

		String normalField = index.binding().identicalForFirstTwo.relativeFieldName;
		String flattenedField = "flattened." + index.binding().flattenedField.relativeFieldName;
		String nestedField = "nested." + index.binding().flattenedField.relativeFieldName;

		query = simpleQuery( f -> f.field( flattenedField ).asc().then().field( normalField ).asc() );
		// [a b a][a a b] => {[1 3] 2}
		assertThat( query.fetch( 2 ) ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery( f -> f.field( nestedField ).asc().then().field( flattenedField ).asc() );
		// [b a a][a b a] => {[3 2] 1}
		assertThat( query.fetch( 2 ) ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_3, DOCUMENT_2 );

		query = simpleQuery( f -> f.field( normalField ).asc().then().field( nestedField ).asc() );
		// [a a b][b a a] => {[2 1] 3}
		assertThat( query.fetch( 2 ) ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	public void then_flattened_nested_filterByPredicate() {
		SearchQuery<DocumentReference> query;

		String normalField = index.binding().identicalForFirstTwo.relativeFieldName;
		String flattenedField = "flattened." + index.binding().flattenedField.relativeFieldName;
		String nestedField = "nested." + index.binding().flattenedField.relativeFieldName;

		query = index.createScope().query()
				.where( b -> b.match().field( normalField ).matching( "aaa" ) )
				.sort( f -> f.field( flattenedField ).asc().then().field( normalField ).asc() )
				.toQuery();

		// [a b a][a a b] => {1+ 3 2+}
		assertThat( query.fetch( 2 ) ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		query = index.createScope().query()
				.where( b -> b.match().field( normalField ).matching( "aaa" ) )
				.sort( f -> f.field( nestedField ).asc().then().field( flattenedField ).asc() )
				.toQuery();

		// [b a a][a b a] => {3 2+ 1+}
		assertThat( query.fetch( 2 ) ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1 );

		query = index.createScope().query()
				.where( b -> b.match().field( normalField ).matching( "aaa" ) )
				.sort( f -> f.field( normalField ).asc().then().field( nestedField ).asc() )
				.toQuery();
		// [a a b][b a a] => {2+ 1+ 3}
		assertThat( query.fetch( 2 ) ).hasDocRefHitsExactOrder( index.typeName(), DOCUMENT_2, DOCUMENT_1 );
	}

	private SearchQuery<DocumentReference> simpleQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return simpleQuery( index.createScope(), sortContributor );
	}

	private SearchQuery<DocumentReference> simpleQuery(StubMappingScope scope,
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private SearchQuery<DocumentReference> simpleQuery(StubMappingScope scope, SearchSort sort) {
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();
	}

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					index.binding().identicalForFirstTwo.write( document, "aaa" );
					index.binding().identicalForLastTwo.write( document, "aaa" );

					DocumentElement flattened = document.addObject( index.binding().flattenedObject );
					index.binding().flattenedField.write( flattened, "aaa" );
					DocumentElement nested = document.addObject( index.binding().nestedObject );
					index.binding().nestedField.write( nested, "bbb" );
				} )
				.add( DOCUMENT_2, document -> {
					index.binding().identicalForFirstTwo.write( document, "aaa" );
					index.binding().identicalForLastTwo.write( document, "bbb" );

					DocumentElement flattened = document.addObject( index.binding().flattenedObject );
					index.binding().flattenedField.write( flattened, "bbb" );
					DocumentElement nested = document.addObject( index.binding().nestedObject );
					index.binding().nestedField.write( nested, "aaa" );
				} )
				.add( DOCUMENT_3, document -> {
					index.binding().identicalForFirstTwo.write( document, "bbb" );
					index.binding().identicalForLastTwo.write( document, "bbb" );

					DocumentElement flattened = document.addObject( index.binding().flattenedObject );
					index.binding().flattenedField.write( flattened, "aaa" );
					DocumentElement nested = document.addObject( index.binding().nestedObject );
					index.binding().nestedField.write( nested, "aaa" );
				} )
				.join();

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexBinding {
		final MainFieldModel<String> identicalForFirstTwo;
		final MainFieldModel<String> identicalForLastTwo;
		final MainFieldModel<String> string3;

		final IndexObjectFieldReference flattenedObject;
		final MainFieldModel<String> flattenedField;
		final IndexObjectFieldReference nestedObject;
		final MainFieldModel<String> nestedField;

		IndexBinding(IndexSchemaElement root) {
			identicalForFirstTwo = MainFieldModel.mapper( f -> f.asString().sortable( Sortable.YES ) )
					.map( root, "identicalForFirstTwo" );
			identicalForLastTwo = MainFieldModel.mapper( f -> f.asString().sortable( Sortable.YES ) )
					.map( root, "identicalForLastTwo" );
			string3 = MainFieldModel.mapper( f -> f.asString() )
					.map( root, "string3" );

			IndexSchemaObjectField flattened = root.objectField( "flattened", ObjectFieldStorage.FLATTENED );
			flattenedObject = flattened.toReference();
			flattenedField = MainFieldModel.mapper( f -> f.asString().sortable( Sortable.YES ) )
					.map( flattened, "field" );

			IndexSchemaObjectField nested = root.objectField( "nested", ObjectFieldStorage.NESTED );
			nestedObject = nested.toReference();
			nestedField = MainFieldModel.mapper( f -> f.asString().sortable( Sortable.YES ) )
					.map( nested, "field" );
		}
	}

	private static class MainFieldModel<T> {
		static <LT> StandardFieldMapper<LT, MainFieldModel<LT>> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, LT>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					(reference, name) -> new MainFieldModel<>( reference, name )
			);
		}

		final IndexFieldReference<T> reference;
		final String relativeFieldName;

		private MainFieldModel(IndexFieldReference<T> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
		}

		void write(DocumentElement documentElement, T value) {
			documentElement.addValue( reference, value );
		}
	}
}
