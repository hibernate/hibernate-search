/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests related to behavior independent from the field type
 * for projections on field value.
 * <p>
 * Behavior that is specific to the field type is tested elsewhere,
 * e.g. {@link FieldProjectionSingleValuedBaseIT} and {@link FieldProjectionTypeCheckingAndConversionIT}.
 */

class FieldProjectionTypeIndependentIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	void nullClass() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( index.binding().string1Field.relativeFieldName, (Class<?>) null )
				.toProjection()
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"must not be null",
						"clazz"
				);
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> string1Field;


		IndexBinding(IndexSchemaElement root) {
			string1Field = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE, c -> {} )
					.map( root, "string1" );
		}
	}

}
