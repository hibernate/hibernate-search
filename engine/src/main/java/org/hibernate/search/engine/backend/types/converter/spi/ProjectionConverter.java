/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContextExtension;

/**
 * A converter from a value obtained from the backend to a projected value.
 *
 * @param <F> The type of source values obtained from the backend.
 * @param <V> The type of projected values.
 */
public final class ProjectionConverter<F, V> {

	private final FromDocumentFieldValueConverter<F, V> delegate;

	public ProjectionConverter(FromDocumentFieldValueConverter<F, V> delegate) {
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegate=" + delegate + "]";
	}

	/**
	 * Check whether converted values can be assigned to the given type.
	 * @param superTypeCandidate A candidate type for assignment of converted values.
	 * @return {@code true} if the converted type {@link V} is a subtype of {@code superTypeCandidate},
	 * {@code false} otherwise.
	 */
	public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
		return delegate.isConvertedTypeAssignableTo( superTypeCandidate );
	}

	/**
	 * @param value The index field value to convert.
	 * @param context A context that can be
	 * {@link FromDocumentFieldValueConvertContext#extension(FromDocumentFieldValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 * @return The converted value.
	 */
	public V convert(F value, FromDocumentFieldValueConvertContext context) {
		return delegate.convert( value, context );
	}

	/**
	 * @param other Another {@link DslConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #isConvertedTypeAssignableTo(Class)} and {@link #convert(Object, FromDocumentFieldValueConvertContext)}
	 * methods are guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	public boolean isCompatibleWith(ProjectionConverter<?, ?> other) {
		return delegate.isCompatibleWith( other.delegate );
	}
}

