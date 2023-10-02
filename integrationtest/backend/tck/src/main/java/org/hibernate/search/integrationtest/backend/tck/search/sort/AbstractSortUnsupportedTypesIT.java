/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractSortUnsupportedTypesIT {

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void trait(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?, ?> fieldTypeDescriptor) {
		String fieldPath = index.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;

		assertThat( index.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.doesNotContain( sortTrait() ) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void use(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?, ?> fieldTypeDescriptor) {
		StubMappingScope scope = index.createScope();
		String fieldPath = index.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;

		assertThatThrownBy( () -> trySort( scope.sort(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + sortTrait() + "' on field '" + fieldPath + "'",
						"'" + sortTrait() + "' is not available for fields of this type"
				);
	}

	protected abstract void trySort(SearchSortFactory f, String fieldPath);

	protected abstract String sortTrait();

	static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;

		IndexBinding(IndexSchemaElement root, List<? extends FieldTypeDescriptor<?, ?>> unsupportedTypes) {
			fieldModels = SimpleFieldModelsByType.< // Necessary for the eclipse compiler
					SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>mapAll( unsupportedTypes, root, "", (f, c) -> {
						if ( f.isFieldSortSupported() && c instanceof StandardIndexFieldTypeOptionsStep ) {
							( (StandardIndexFieldTypeOptionsStep<?, ?>) c ).sortable( Sortable.YES );
						}
					} );
		}
	}

}
