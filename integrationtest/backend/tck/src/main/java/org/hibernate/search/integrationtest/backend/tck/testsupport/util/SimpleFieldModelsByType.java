/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public class SimpleFieldModelsByType {
	@SafeVarargs
	public static SimpleFieldModelsByType mapAll(Stream<FieldTypeDescriptor<?>> typeDescriptors,
			IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration1,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> ... additionalConfiguration2) {
		SimpleFieldModelsByType result = new SimpleFieldModelsByType();
		typeDescriptors.forEach( typeDescriptor -> {
			result.content.put(
					typeDescriptor,
					SimpleFieldModel.mapper( typeDescriptor, additionalConfiguration1 )
							.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration2 )
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
