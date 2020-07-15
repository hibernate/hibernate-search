/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * A converter for values passed to the DSL.
 *
 * @param <V> The type of values passed to the DSL.
 * @param <F> The type of converted values passed to the backend.
 */
public final class DslConverter<V, F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Class<V> valueType;
	private final ToDocumentFieldValueConverter<V, ? extends F> delegate;

	public DslConverter(Class<V> valueType, ToDocumentFieldValueConverter<V, ? extends F> delegate) {
		Contracts.assertNotNull( valueType, "valueType" );
		Contracts.assertNotNull( delegate, "delegate" );
		this.valueType = valueType;
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[valueType=" + valueType.getName() + ",delegate=" + delegate + "]";
	}

	public Class<V> valueType() {
		return valueType;
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
	 * Check whether DSL arguments values can have the given type,
	 * and returns the DSL converter with an appropriate type.
	 *
	 * @param inputTypeCandidate A candidate type for input values.
	 * @param eventContextProvider A provider for the event context to pass to produced exceptions.
	 * @return The DSL converter, guaranteed to accept values to the given type.
	 * @throws org.hibernate.search.util.common.SearchException If the DSL converter cannot accept values to the given type.
	 * @param <T> A candidate type for input values.
	 */
	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	public <T> DslConverter<? super T, F> withInputType(Class<T> inputTypeCandidate,
			EventContextProvider eventContextProvider) {
		if ( !valueType.isAssignableFrom( inputTypeCandidate ) ) {
			throw log.invalidDslArgumentType( inputTypeCandidate, valueType, eventContextProvider.eventContext() );
		}
		return (DslConverter<? super T, F>) this;
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
