/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.util.function.Function;

import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A pass-through value bridge, i.e. a bridge that passes the input value as-is to the underlying backend.
 * <p>
 * This bridge will not work for any type: only types supported by the backend
 * through {@link IndexFieldTypeFactory#as(Class)} will work.
 *
 * @param <F> The type of input values, as well as the type of the index field.
 */
public final class PassThroughValueBridge<F> implements ValueBridge<F, F> {

	private final Class<F> fieldType;
	private final Function<String, F> parsingFunction;

	public PassThroughValueBridge(Class<F> fieldType, Function<String, F> parsingFunction) {
		Contracts.assertNotNull( fieldType, "fieldType" );
		this.fieldType = fieldType;
		this.parsingFunction = parsingFunction;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + fieldType.getName() + "]";
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, F> bind(ValueBridgeBindingContext<F> context) {
		return context.getTypeFactory().as( fieldType )
				.projectionConverter( new PassThroughFromDocumentFieldValueConverter<>( fieldType ) );
	}

	@Override
	public F cast(Object value) {
		return fieldType.cast( value );
	}

	@Override
	public F parse(String value) {
		return parsingFunction.apply( value );
	}

	@Override
	public F toIndexedValue(F value,
			ValueBridgeToIndexedValueContext context) {
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PassThroughValueBridge<?> castedOther = (PassThroughValueBridge<?>) other;
		return fieldType.equals( castedOther.fieldType );
	}
}