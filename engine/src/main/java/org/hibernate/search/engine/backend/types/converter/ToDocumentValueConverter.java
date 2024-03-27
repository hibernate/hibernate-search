/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter;

import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContextExtension;

/**
 * A converter from a source value to a target value in the document model.
 *
 * @param <V> The type of source values.
 * @param <F> The type of target values in the document model.
 */
public interface ToDocumentValueConverter<V, F> {

	/**
	 * @param value The source value to convert to the document model.
	 * @param context A context that can be
	 * {@link ToDocumentValueConvertContext#extension(ToDocumentValueConvertContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The converted index field value.
	 */
	F toDocumentValue(V value, ToDocumentValueConvertContext context);

	/**
	 * @param other Another {@link ToDocumentValueConverter}, never {@code null}.
	 * @return {@code true} if the given object behaves exactly the same as this object,
	 * i.e. its {@link #toDocumentValue(Object, ToDocumentValueConvertContext)}
	 * method is guaranteed to always return the same value as this object's
	 * when given the same input. {@code false} otherwise, or when in doubt.
	 */
	default boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
		return equals( other );
	}

}
