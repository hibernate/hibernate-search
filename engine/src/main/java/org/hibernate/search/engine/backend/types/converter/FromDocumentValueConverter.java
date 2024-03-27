/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContextExtension;

/**
 * A converter from a source value in the document model to a different value.
 *
 * @param <F> The type of source values in the document model.
 * @param <V> The type of target values.
 */
public interface FromDocumentValueConverter<F, V> {

	/**
	 * @param value The value to convert from the document model.
	 * @param context A context that can be
	 * {@link FromDocumentValueConvertContext#extension(FromDocumentValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 * @return The converted value.
	 */
	V fromDocumentValue(F value, FromDocumentValueConvertContext context);

	/**
	 * @param other Another {@link FromDocumentValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #fromDocumentValue(Object, FromDocumentValueConvertContext)}
	 * method is guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
		return equals( other );
	}

}
