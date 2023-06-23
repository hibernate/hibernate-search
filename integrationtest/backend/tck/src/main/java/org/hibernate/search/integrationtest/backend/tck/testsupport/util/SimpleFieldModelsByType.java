/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public class SimpleFieldModelsByType {
	public static SimpleFieldModelsByType mapAll(Collection<? extends FieldTypeDescriptor<?>> typeDescriptors,
			IndexSchemaElement parent, String prefix) {
		return mapAll( typeDescriptors.stream(), parent, prefix );
	}

	@SafeVarargs
	public static SimpleFieldModelsByType mapAll(Collection<? extends FieldTypeDescriptor<?>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>>... additionalConfiguration) {
		return mapAll( typeDescriptors.stream(), parent, prefix, additionalConfiguration );
	}

	@SafeVarargs
	public static SimpleFieldModelsByType mapAll(Stream<? extends FieldTypeDescriptor<?>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>>... additionalConfiguration) {
		SimpleFieldModelsByType result = new SimpleFieldModelsByType();
		typeDescriptors.forEach( typeDescriptor -> {
			result.content.put(
					typeDescriptor,
					SimpleFieldModel.mapper( typeDescriptor, ignored -> {} )
							.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration )
			);
		} );
		return result;
	}

	@SafeVarargs
	public static SimpleFieldModelsByType mapAll(Collection<? extends FieldTypeDescriptor<?>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			BiConsumer<FieldTypeDescriptor<?>, StandardIndexFieldTypeOptionsStep<?, ?>>... additionalConfiguration) {
		SimpleFieldModelsByType result = new SimpleFieldModelsByType();
		typeDescriptors.forEach( typeDescriptor -> {
			result.content.put(
					typeDescriptor,
					SimpleFieldModel.mapper( typeDescriptor, c -> {
						for ( BiConsumer<FieldTypeDescriptor<?>,
								StandardIndexFieldTypeOptionsStep<?, ?>> config : additionalConfiguration ) {
							config.accept( typeDescriptor, c );
						}
					} )
							.map( parent, prefix + typeDescriptor.getUniqueName() )
			);
		} );
		return result;
	}

	@SafeVarargs
	public static SimpleFieldModelsByType mapAllMultiValued(Collection<? extends FieldTypeDescriptor<?>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>>... additionalConfiguration) {
		return mapAllMultiValued( typeDescriptors.stream(), parent, prefix, additionalConfiguration );
	}

	@SafeVarargs
	public static SimpleFieldModelsByType mapAllMultiValued(Stream<? extends FieldTypeDescriptor<?>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>>... additionalConfiguration) {
		SimpleFieldModelsByType result = new SimpleFieldModelsByType();
		typeDescriptors.forEach( typeDescriptor -> {
			result.content.put(
					typeDescriptor,
					SimpleFieldModel.mapper( typeDescriptor, ignored -> {} )
							.mapMultiValued( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration )
			);
		} );
		return result;
	}

	private final Map<FieldTypeDescriptor<?>, SimpleFieldModel<?>> content = new LinkedHashMap<>();

	@Override
	public String toString() {
		return "SimpleFieldModelsByType[" + content + "]";
	}

	@SuppressWarnings("unchecked")
	public <F> SimpleFieldModel<F> get(FieldTypeDescriptor<F> typeDescriptor) {
		return (SimpleFieldModel<F>) content.get( typeDescriptor );
	}

	public void forEach(Consumer<SimpleFieldModel<?>> action) {
		content.values().forEach( action );
	}
}
