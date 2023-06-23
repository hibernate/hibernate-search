/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public class SimpleFieldModel<F> {

	public static <F> StandardFieldMapper<F, SimpleFieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
		return mapper( typeDescriptor, ignored -> {} );
	}

	public static <F> StandardFieldMapper<F, SimpleFieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor,
			Consumer<? super StandardIndexFieldTypeOptionsStep<?, F>> configurationAdjustment) {
		return StandardFieldMapper.of(
				typeDescriptor::configure,
				configurationAdjustment,
				(reference, relativeFieldName) -> new SimpleFieldModel<>( typeDescriptor, reference, relativeFieldName )
		);
	}

	public static <F> StandardFieldMapper<F, SimpleFieldModel<F>> mapperWithOverride(FieldTypeDescriptor<F> typeDescriptor,
			Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> initialConfiguration) {
		return StandardFieldMapper.of(
				initialConfiguration,
				(reference, relativeFieldName) -> new SimpleFieldModel<>( typeDescriptor, reference, relativeFieldName )
		);
	}

	public final FieldTypeDescriptor<F> typeDescriptor;
	public final String relativeFieldName;
	public final IndexFieldReference<F> reference;

	private SimpleFieldModel(FieldTypeDescriptor<F> typeDescriptor, IndexFieldReference<F> reference,
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
