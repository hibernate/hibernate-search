/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A converter for values passed to the DSL.
 *
 * @param <V> The type of values passed to the DSL.
 * @param <F> The type of converted values passed to the backend.
 */
public final class DslConverter<V, F> {
	private final Class<V> valueType;
	private final ToDocumentFieldValueConverter<V, F> delegate;

	public DslConverter(Class<V> valueType, ToDocumentFieldValueConverter<V, F> delegate) {
		Contracts.assertNotNull( valueType, "valueType" );
		Contracts.assertNotNull( delegate, "delegate" );
		this.valueType = valueType;
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[valueType=" + valueType.getName() + ",delegate=" + delegate + "]";
	}

	/**
	 * Check whether the given type is a valid type for values passed
	 * to {@link #convert(Object, ToDocumentFieldValueConvertContext)},
	 * which generally means the given type is a subtype of {@link V}.
	 * @param inputTypeCandidate A candidate type for the input of {@link #convertUnknown(Object, ToDocumentFieldValueConvertContext)}.
	 * @return {@code true} if values of type {@code inputTypeCandidate}
	 * may be accepted by {@link #convertUnknown(Object, ToDocumentFieldValueConvertContext)},
	 * {@code false} otherwise.
	 */
	public boolean isValidInputType(Class<?> inputTypeCandidate) {
		return valueType.isAssignableFrom( inputTypeCandidate );
	}

	/**
	 * @param value The source value to convert.
	 * @param context A context that can be
	 * {@link ToDocumentFieldValueConvertContext#extension(ToDocumentFieldValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 */
	public F convert(V value, ToDocumentFieldValueConvertContext context) {
		return delegate.convert( value, context );
	}

	/**
	 * Convert an input value of unknown type that may not have the required type {@code V}.
	 * <p>
	 * Called when passing values to the predicate DSL in particular.
	 *
	 * @param value The value to convert.
	 * @param context A context that can be
	 * {@link ToDocumentFieldValueConvertContext#extension(ToDocumentFieldValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 * @throws RuntimeException If the value does not match the expected type.
	 */
	public F convertUnknown(Object value, ToDocumentFieldValueConvertContext context) {
		return delegate.convert( valueType.cast( value ), context );
	}

	/**
	 * @param other Another {@link DslConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #convert(Object, ToDocumentFieldValueConvertContext)} and {@link #convertUnknown(Object, ToDocumentFieldValueConvertContext)}
	 * methods are guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	public boolean isCompatibleWith(DslConverter<?, ?> other) {
		return delegate.isCompatibleWith( other.delegate );
	}
}
