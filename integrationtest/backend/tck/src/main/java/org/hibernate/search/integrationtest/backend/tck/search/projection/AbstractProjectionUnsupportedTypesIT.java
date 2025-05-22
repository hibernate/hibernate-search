/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractProjectionUnsupportedTypesIT {

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void trait(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?, ?> fieldTypeDescriptor) {
		String fieldPath = index.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;

		assertThat( index.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.doesNotContain( projectionTrait() ) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void use(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?, ?> fieldTypeDescriptor) {
		StubMappingScope scope = index.createScope();
		String fieldPath = index.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;

		assertThatThrownBy( () -> tryProjection( scope.projection(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use '" + projectionTrait() + "' on field '" + fieldPath + "'",
						"'" + projectionTrait() + "' is not available for fields of this type"
				);
	}

	protected abstract void tryProjection(SearchProjectionFactory<?, ?> f, String fieldPath);

	protected abstract String projectionTrait();

	static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;

		IndexBinding(IndexSchemaElement root,
				List<FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> unsupportedTypes) {
			fieldModels = SimpleFieldModelsByType.mapAll( unsupportedTypes, root, "", c -> {
				if ( !TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault() ) {
					c.projectable( Projectable.YES );
				}
			} );
		}
	}

}
