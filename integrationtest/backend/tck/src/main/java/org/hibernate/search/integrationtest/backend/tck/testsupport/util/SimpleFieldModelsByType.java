/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public class SimpleFieldModelsByType {
	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SimpleFieldModelsByType mapAll(
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> typeDescriptors,
			IndexSchemaElement parent, String prefix) {
		return mapAll( typeDescriptors.stream(), parent, prefix );
	}

	@SafeVarargs
	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SimpleFieldModelsByType mapAll(
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<? super S>... additionalConfiguration) {
		return mapAll( typeDescriptors.stream(), parent, prefix, additionalConfiguration );
	}

	@SafeVarargs
	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SimpleFieldModelsByType mapAll(
			Stream<? extends FieldTypeDescriptor<?, ? extends S>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<? super S>... additionalConfiguration) {
		SimpleFieldModelsByType result = new SimpleFieldModelsByType();
		typeDescriptors.forEach( typeDescriptor -> {
			result.content.put(
					typeDescriptor,
					map( typeDescriptor, parent, prefix, (S step) -> {
						for ( Consumer<? super S> config : additionalConfiguration ) {
							config.accept( step );
						}
					} )
			);
		} );
		return result;
	}

	@SafeVarargs
	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SimpleFieldModelsByType mapAll(
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			BiConsumer<FieldTypeDescriptor<?, ? extends S>, S>... additionalConfiguration) {
		SimpleFieldModelsByType result = new SimpleFieldModelsByType();
		typeDescriptors.forEach( typeDescriptor -> {
			result.content.put(
					typeDescriptor,
					map( typeDescriptor, parent, prefix, (S step) -> {
						for ( BiConsumer<FieldTypeDescriptor<?, ? extends S>, S> config : additionalConfiguration ) {
							config.accept( typeDescriptor, step );
						}
					} )
			);
		} );
		return result;
	}

	// We can't easily express "S2 extends S and SearchableProjectableIndexFieldTypeOptionsStep<?, F>" in java
	// so we fall back to dirty, dirty casts...
	@SuppressWarnings("unchecked")
	private static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SimpleFieldModel<?> map(
			FieldTypeDescriptor<?, ? extends S> typeDescriptor, IndexSchemaElement parent, String prefix,
			Consumer<S> additionalConfiguration) {
		return SimpleFieldModel.mapper( (FieldTypeDescriptor<?, ?>) typeDescriptor )
				.map( parent, prefix + typeDescriptor.getUniqueName(),
						step -> additionalConfiguration.accept( (S) step ) );
	}

	@SafeVarargs
	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SimpleFieldModelsByType mapAllMultiValued(
			Collection<? extends FieldTypeDescriptor<?, ? extends S>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<? super S>... additionalConfiguration) {
		return mapAllMultiValued( typeDescriptors.stream(), parent, prefix, additionalConfiguration );
	}

	@SafeVarargs
	public static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SimpleFieldModelsByType mapAllMultiValued(
			Stream<? extends FieldTypeDescriptor<?, ? extends S>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<? super S>... additionalConfiguration) {
		SimpleFieldModelsByType result = new SimpleFieldModelsByType();
		typeDescriptors.forEach( typeDescriptor -> {
			result.content.put(
					typeDescriptor,
					mapMultiValued( typeDescriptor, parent, prefix, additionalConfiguration )
			);
		} );
		return result;
	}

	@SuppressWarnings("unchecked")
	private static <S extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> SimpleFieldModel<?> mapMultiValued(
			FieldTypeDescriptor<?, ? extends S> typeDescriptor, IndexSchemaElement parent, String prefix,
			Consumer<? super S>[] additionalConfiguration) {
		return SimpleFieldModel.mapper( (FieldTypeDescriptor<?, ?>) typeDescriptor )
				.mapMultiValued( parent, prefix + typeDescriptor.getUniqueName(),
						step -> {
							for ( Consumer<? super S> config : additionalConfiguration ) {
								config.accept( (S) step );
							}
						} );
	}

	private final Map<FieldTypeDescriptor<?, ?>, SimpleFieldModel<?>> content = new LinkedHashMap<>();

	@Override
	public String toString() {
		return "SimpleFieldModelsByType[" + content + "]";
	}

	@SuppressWarnings("unchecked")
	public <F> SimpleFieldModel<F> get(FieldTypeDescriptor<F, ?> typeDescriptor) {
		return (SimpleFieldModel<F>) content.get( typeDescriptor );
	}

	public void forEach(Consumer<SimpleFieldModel<?>> action) {
		content.values().forEach( action );
	}
}
