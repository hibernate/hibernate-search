/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractSortSortableIT {

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void sortable_default_trait(
			SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
			SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
			SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		String fieldPath = sortableDefaultIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThat( sortableDefaultIndex.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.doesNotContain( sortTrait() ) );
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void sortable_yes_trait(
			SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
			SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
			SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		String fieldPath = sortableYesIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThat( sortableYesIndex.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.contains( sortTrait() ) );
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void sortable_no_trait(
			SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
			SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
			SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		String fieldPath = sortableNoIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThat( sortableNoIndex.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.doesNotContain( sortTrait() ) );
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void sortable_default_use(
			SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
			SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
			SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchSortFactory f = sortableDefaultIndex.createScope().sort();

		String fieldPath = sortableDefaultIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> trySort( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + sortTrait() + "' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void sortable_no_use(
			SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
			SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
			SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchSortFactory f = sortableNoIndex.createScope().sort();

		String fieldPath = sortableNoIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> trySort( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + sortTrait() + "' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}


	@ParameterizedTest(name = "{3}")
	@MethodSource("params")
	void multiIndex_incompatibleSortable(
			SimpleMappedIndex<SortableDefaultIndexBinding> sortableDefaultIndex,
			SimpleMappedIndex<SortableYesIndexBinding> sortableYesIndex,
			SimpleMappedIndex<SortableNoIndexBinding> sortableNoIndex,
			FieldTypeDescriptor<?, ?> fieldType) {
		SearchSortFactory f = sortableYesIndex.createScope( sortableNoIndex ).sort();

		String fieldPath = sortableYesIndex.binding().field.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> trySort( f, fieldPath, fieldType ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + sortTrait() + "'"
				);
	}

	protected abstract void trySort(SearchSortFactory f, String fieldPath,
			FieldTypeDescriptor<?, ?> fieldType);

	protected abstract String sortTrait();

	public static final class SortableDefaultIndexBinding {
		final SimpleFieldModelsByType field;

		public SortableDefaultIndexBinding(IndexSchemaElement root, Collection<
				? extends StandardFieldTypeDescriptor<?>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "" );
		}
	}

	public static final class SortableYesIndexBinding {
		private final SimpleFieldModelsByType field;

		public SortableYesIndexBinding(IndexSchemaElement root, Collection<
				? extends StandardFieldTypeDescriptor<?>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.sortable( Sortable.YES ) );
		}
	}

	public static final class SortableNoIndexBinding {
		final SimpleFieldModelsByType field;

		public SortableNoIndexBinding(IndexSchemaElement root, Collection<
				? extends StandardFieldTypeDescriptor<?>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.sortable( Sortable.NO ) );
		}
	}

}
