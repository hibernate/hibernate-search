/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter;

/**
 * Defines how a given value will be converted when performing search queries.
 * <p>
 * Used by predicate and sort builders in particular, and also by hit extractors when projecting in a search query.
 *
 * @param <F> The type of the index field.
 * @param <T> The type used internally when querying. May be different from the field type exposed to users;
 * see for example {@link LocalDateFieldConverter}.
 */
public interface LuceneFieldConverter<F, T> {

	/**
	 * @param value A value passed through the predicate or sort DSL.
	 * @param context The context to use when converting.
	 * @return A value of the type used internally when querying this field.
	 * @throws RuntimeException If the value does not match the expected type.
	 */
	T convertFromDsl(Object value, ToIndexFieldValueConvertContext context);

	/**
	 * @param value The projected value returned by the codec.
	 * @param context The context to use when converting.
	 * @return A value of the type expected by users when projecting.
	 */
	Object convertFromProjection(F value, FromIndexFieldValueConvertContext context);

	/**
	 * Determine whether another converter is DSL-compatible with this one.
	 *
	 * @see org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter#isDslCompatibleWith(UserIndexFieldConverter)
	 *
	 * @param other Another {@link LuceneFieldConverter}, never {@code null}.
	 * @return {@code true} if the given converter is DSL-compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isDslCompatibleWith(LuceneFieldConverter<?, ?> other);

	/**
	 * Determine whether the given projection type is compatible with this converter.
	 *
	 * @param projectionType The projection type.
	 * @return {@code true} if the given projection type is compatible. {@code false} otherwise
	 */
	boolean isProjectionCompatibleWith(Class<?> projectionType);

}
