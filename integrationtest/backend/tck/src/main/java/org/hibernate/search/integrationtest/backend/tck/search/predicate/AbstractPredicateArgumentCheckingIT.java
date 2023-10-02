/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateArgumentCheckingIT {

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void nullMatchingParam(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?, ?> fieldType) {
		SearchPredicateFactory f = index.createScope().predicate();

		assertThatThrownBy( () -> tryPredicateWithNullMatchingParam( f, fieldPath( index, fieldType ) ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll( "must not be null" );
	}

	protected abstract void tryPredicateWithNullMatchingParam(SearchPredicateFactory f, String fieldPath);

	protected String fieldPath(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?, ?> fieldType) {
		return index.binding().field.get( fieldType ).relativeFieldName;
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType field;

		public IndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "" );
		}
	}

}
