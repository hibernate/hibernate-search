/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public class SimpleFieldModel<F> {

	public static <
			F,
			S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>> SimpleFieldMapper<F, S, SimpleFieldModel<F>> mapper(
					FieldTypeDescriptor<F, S> typeDescriptor) {
		return mapper( typeDescriptor, ignored -> {} );
	}

	public static <
			F,
			S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>> SimpleFieldMapper<F, S, SimpleFieldModel<F>> mapper(
					FieldTypeDescriptor<F, ? extends S> typeDescriptor,
					Consumer<S> configurationAdjustment) {
		return SimpleFieldMapper.of(
				typeDescriptor::configure,
				configurationAdjustment,
				(reference, relativeFieldName) -> new SimpleFieldModel<>( typeDescriptor, reference, relativeFieldName )
		);
	}

	public static <
			F,
			S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>> SimpleFieldMapper<F,
					S,
					SimpleFieldModel<F>> mapperWithOverride(
							FieldTypeDescriptor<F, S> typeDescriptor,
							Function<IndexFieldTypeFactory, S> initialConfiguration) {
		return SimpleFieldMapper.of(
				initialConfiguration,
				(reference, relativeFieldName) -> new SimpleFieldModel<>( typeDescriptor, reference, relativeFieldName )
		);
	}

	public final FieldTypeDescriptor<F, ?> typeDescriptor;
	public final String relativeFieldName;
	public final IndexFieldReference<F> reference;

	private SimpleFieldModel(FieldTypeDescriptor<F, ?> typeDescriptor, IndexFieldReference<F> reference,
			String relativeFieldName) {
		this.typeDescriptor = typeDescriptor;
		this.reference = reference;
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	public String toString() {
		return "SimpleFieldModel["
				+ "typeDescriptor=" + typeDescriptor
				+ ", relativeFieldName=" + relativeFieldName
				+ "]";
	}
}
