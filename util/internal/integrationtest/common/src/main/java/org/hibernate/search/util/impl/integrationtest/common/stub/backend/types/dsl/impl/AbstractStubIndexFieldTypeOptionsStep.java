/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl.StubFieldConverter;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexFieldType;

abstract class AbstractStubIndexFieldTypeOptionsStep<S extends AbstractStubIndexFieldTypeOptionsStep<?, F>, F>
		implements IndexFieldTypeOptionsStep<S, F> {

	final List<Consumer<StubIndexSchemaNode.Builder>> modifiers;
	private final Class<F> inputType;

	private DslConverter<?, ? extends F> dslConverter;
	private ProjectionConverter<? super F, ?> projectionConverter;

	AbstractStubIndexFieldTypeOptionsStep(Class<F> inputType) {
		this.modifiers = new ArrayList<>();
		this.inputType = inputType;
		modifiers.add(
				b -> b.converter( new StubFieldConverter<>(
						inputType,
						dslConverter == null
								? new DslConverter<>( inputType, new PassThroughToDocumentFieldValueConverter<>() )
								: dslConverter,
						projectionConverter == null
								? new ProjectionConverter<>(
								inputType, new PassThroughFromDocumentFieldValueConverter<>() )
								: projectionConverter
				) )
		);
	}

	abstract S thisAsS();

	@Override
	public <V> S dslConverter(Class<V> valueType, ToDocumentFieldValueConverter<V, ? extends F> toIndexConverter) {
		this.dslConverter = new DslConverter<>( valueType, toIndexConverter );
		return thisAsS();
	}

	@Override
	public <V> S projectionConverter(Class<V> valueType, FromDocumentFieldValueConverter<? super F, V> fromIndexConverter) {
		this.projectionConverter = new ProjectionConverter<>( valueType, fromIndexConverter );
		return thisAsS();
	}

	@Override
	public IndexFieldType<F> toIndexFieldType() {
		return new StubIndexFieldType<>( inputType, modifiers );
	}

}
