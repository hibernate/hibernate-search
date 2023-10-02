/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateSearchableIT {

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void searchable_default_trait(SimpleMappedIndex<SearchableDefaultIndexBinding> searchableDefaultIndex,
			SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
			SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		String fieldPath = searchableDefaultIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThat( searchableDefaultIndex.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.contains( predicateTrait() ) );
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void searchable_yes_trait(SimpleMappedIndex<SearchableDefaultIndexBinding> searchableDefaultIndex,
			SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
			SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		String fieldPath = searchableYesIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThat( searchableYesIndex.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.contains( predicateTrait() ) );
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void searchable_no_trait(SimpleMappedIndex<SearchableDefaultIndexBinding> searchableDefaultIndex,
			SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
			SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		String fieldPath = searchableNoIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThat( searchableNoIndex.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.doesNotContain( predicateTrait() ) );
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void searchable_no_use(SimpleMappedIndex<SearchableDefaultIndexBinding> searchableDefaultIndex,
			SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
			SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchPredicateFactory f = searchableNoIndex.createScope().predicate();

		String fieldPath = searchableNoIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryPredicate( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + predicateTrait() + "' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void multiIndex_incompatibleSearchable(SimpleMappedIndex<SearchableDefaultIndexBinding> searchableDefaultIndex,
			SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
			SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchPredicateFactory f = searchableYesIndex.createScope( searchableNoIndex ).predicate();

		String fieldPath = searchableYesIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> tryPredicate( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateTrait() + "'"
				);
	}

	protected abstract void tryPredicate(SearchPredicateFactory f, String fieldPath,
			FieldTypeDescriptor<?, ?> fieldType);

	protected abstract String predicateTrait();

	public static final class SearchableDefaultIndexBinding {
		private final SimpleFieldModelsByType field;

		public SearchableDefaultIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "" );
		}
	}

	public static final class SearchableYesIndexBinding {
		private final SimpleFieldModelsByType field;

		public SearchableYesIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.searchable( Searchable.YES ) );
		}
	}

	public static final class SearchableNoIndexBinding {
		final SimpleFieldModelsByType field;

		public SearchableNoIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.searchable( Searchable.NO ) );
		}
	}

}
