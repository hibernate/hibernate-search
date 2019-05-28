/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.searchable;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FieldSearchableIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private SearchableFieldsIndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private StubMappingIndexManager compatibleIndexManager;

	private StubMappingIndexManager incompatibleIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration( "my-backend" )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new SearchableFieldsIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> new SearchableFieldsIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME,
						ctx -> new NotSearchableFieldsIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.setup();
	}

	@Test
	public void searchable() {
		StubMappingSearchScope scope = indexManager.createSearchScope();
		for ( FieldModel<?> field : indexMapping.fields ) {
			SearchQuery<DocumentReference> query = scope.query().asReference()
					.predicate( p -> p.match().onField( field.name ).matching( field.value ) )
					.toQuery();

			assertThat( query ).hasNoHits();
		}
	}

	@Test
	public void unsearchable() {
		StubMappingSearchScope scope = incompatibleIndexManager.createSearchScope();
		for ( FieldModel<?> field : indexMapping.fields ) {
			SubTest.expectException( () ->
					scope.query().asReference().predicate( p -> p.match().onField( field.name ).matching( field.value ) )
			).assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "is not searchable" )
					.hasMessageContaining( "Make sure the field is marked as searchable" )
					.hasMessageContaining( field.name );
		}
	}

	@Test
	public void multi_indexes_compatible() {
		StubMappingSearchScope scope = indexManager.createSearchScope( compatibleIndexManager );
		for ( FieldModel<?> field : indexMapping.fields ) {
			SearchQuery<DocumentReference> query = scope.query().asReference()
					.predicate( p -> p.match().onField( field.name ).matching( field.value ) )
					.toQuery();

			assertThat( query ).hasNoHits();
		}
	}

	@Test
	public void multi_indexes_incompatible() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleIndexManager );
		for ( FieldModel<?> field : indexMapping.fields ) {
			SubTest.expectException( () ->
					scope.query().asReference().predicate( p -> p.match().onField( field.name ).matching( field.value ) )
			).assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate for field" )
					.hasMessageContaining( field.name )
					.hasMessageContaining( INDEX_NAME )
					.hasMessageContaining( INCOMPATIBLE_INDEX_NAME );
		}
	}

	private static class SearchableFieldsIndexMapping {
		final List<FieldModel<?>> fields;

		SearchableFieldsIndexMapping(IndexSchemaElement root) {
			fields = FieldTypeDescriptor.getAll().stream()
					.filter( type -> type.getMatchPredicateExpectations().isPresent() )
					.filter( type -> type.getMatchPredicateExpectations().get().isMatchPredicateSupported() )
					.map( type -> new FieldModel<>( root, type, Searchable.YES ) )
					.collect( Collectors.toList() );
		}
	}

	private static class NotSearchableFieldsIndexMapping {
		final List<FieldModel<?>> fields;

		NotSearchableFieldsIndexMapping(IndexSchemaElement root) {
			fields = FieldTypeDescriptor.getAll().stream()
					.filter( type -> type.getMatchPredicateExpectations().isPresent() )
					.filter( type -> type.getMatchPredicateExpectations().get().isMatchPredicateSupported() )
					.map( type -> new FieldModel<>( root, type, Searchable.NO ) )
					.collect( Collectors.toList() );
		}
	}

	private static class FieldModel<T> {
		String name;
		IndexFieldReference<T> reference;
		T value;

		FieldModel(IndexSchemaElement root, FieldTypeDescriptor<T> type, Searchable searchable) {
			name = type.getUniqueName();
			reference = root.field( type.getUniqueName(), f -> type.configure( f ).searchable( searchable ) ).toReference();
			Optional<MatchPredicateExpectations<T>> expectations = type.getMatchPredicateExpectations();
			value = ( expectations.isPresent() ) ? expectations.get().getDocument1Value() : null;
		}
	}
}
