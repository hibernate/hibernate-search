/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

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
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.search.dsl.sort.SortFinalStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CompositeSearchSortIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

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
	public void byComposite() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( f -> f.composite( c -> {
			c.add( f.field( indexMapping.identicalForFirstTwo.relativeFieldName ).asc() );
			c.add( f.field( indexMapping.identicalForLastTwo.relativeFieldName ).asc() );
		} ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery( f -> f.composite( c -> {
			c.add( f.field( indexMapping.identicalForFirstTwo.relativeFieldName ).desc() );
			c.add( f.field( indexMapping.identicalForLastTwo.relativeFieldName ).desc() );
		} ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( f -> f.composite( c -> {
			c.add( f.field( indexMapping.identicalForFirstTwo.relativeFieldName ).asc() );
			c.add( f.field( indexMapping.identicalForLastTwo.relativeFieldName ).desc() );
		} ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery( f -> f.composite( c -> {
			c.add( f.field( indexMapping.identicalForFirstTwo.relativeFieldName ).desc() );
			c.add( f.field( indexMapping.identicalForLastTwo.relativeFieldName ).asc() );
		} ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void byComposite_separateSort() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query;

		query = simpleQuery(
				scope,
				scope.sort().composite()
						.add( scope.sort().field( indexMapping.identicalForFirstTwo.relativeFieldName ).asc() )
						.add( scope.sort().field( indexMapping.identicalForLastTwo.relativeFieldName ).asc() )
						.toSort()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery(
				scope,
				scope.sort().composite()
						.add( scope.sort().field( indexMapping.identicalForFirstTwo.relativeFieldName ).desc() )
						.add( scope.sort().field( indexMapping.identicalForLastTwo.relativeFieldName ).desc() )
						.toSort()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery(
				scope,
				scope.sort().composite()
						.add( scope.sort().field( indexMapping.identicalForFirstTwo.relativeFieldName ).asc() )
						.add( scope.sort().field( indexMapping.identicalForLastTwo.relativeFieldName ).desc() )
						.toSort()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery(
				scope,
				scope.sort().composite()
						.add( scope.sort().field( indexMapping.identicalForFirstTwo.relativeFieldName ).desc() )
						.add( scope.sort().field( indexMapping.identicalForLastTwo.relativeFieldName ).asc() )
						.toSort()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void byComposite_empty() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( f -> f.composite() );

		// Just check that the query is executed (the empty sort is ignored and nothing fails)
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void then() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( f -> f
				.field( indexMapping.identicalForFirstTwo.relativeFieldName ).asc()
				.then().field( indexMapping.identicalForLastTwo.relativeFieldName ).asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery( f -> f
				.field( indexMapping.identicalForFirstTwo.relativeFieldName ).desc()
				.then().field( indexMapping.identicalForLastTwo.relativeFieldName ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( f -> f
				.field( indexMapping.identicalForFirstTwo.relativeFieldName ).asc()
				.then().field( indexMapping.identicalForLastTwo.relativeFieldName ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery( f -> f
				.field( indexMapping.identicalForFirstTwo.relativeFieldName ).desc()
				.then().field( indexMapping.identicalForLastTwo.relativeFieldName ).asc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void then_flattened() {
		SearchQuery<DocumentReference> query = simpleQuery( f -> f
				.byField( "flattened." + indexMapping.flattenedField.relativeFieldName ).asc()
				.then().byField( indexMapping.identicalForFirstTwo.relativeFieldName ).asc()
		);
		// [a b a][a a b] => {1 3 2}
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3, DOCUMENT_2 );
	}

	private SearchQuery<DocumentReference> simpleQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return simpleQuery( indexManager.createScope(), sortContributor );
	}

	private SearchQuery<DocumentReference> simpleQuery(StubMappingScope scope,
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return scope.query()
				.predicate( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private SearchQuery<DocumentReference> simpleQuery(StubMappingScope scope, SearchSort sort) {
		return scope.query()
				.predicate( f -> f.matchAll() )
				.sort( sort )
				.toQuery();
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.identicalForFirstTwo.write( document, "aaa" );
			indexMapping.identicalForLastTwo.write( document, "aaa" );

			DocumentElement flattened = document.addObject( indexMapping.flattenedObject );
			indexMapping.flattenedField.write( flattened, "aaa" );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.identicalForFirstTwo.write( document, "aaa" );
			indexMapping.identicalForLastTwo.write( document, "bbb" );

			DocumentElement flattened = document.addObject( indexMapping.flattenedObject );
			indexMapping.flattenedField.write( flattened, "bbb" );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.identicalForFirstTwo.write( document, "bbb" );
			indexMapping.identicalForLastTwo.write( document, "bbb" );

			DocumentElement flattened = document.addObject( indexMapping.flattenedObject );
			indexMapping.flattenedField.write( flattened, "aaa" );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexMapping {
		final MainFieldModel<String> identicalForFirstTwo;
		final MainFieldModel<String> identicalForLastTwo;
		final MainFieldModel<String> string3;

		final IndexObjectFieldReference flattenedObject;
		final MainFieldModel<String> flattenedField;

		IndexMapping(IndexSchemaElement root) {
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
