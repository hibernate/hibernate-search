/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter;

import com.google.gson.JsonElement;

/**
 * Defines how a given value will be converted when performing search queries.
 * <p>
 * Used by predicate and sort builders in particular, and also by hit extractors when projecting in a search query.
 */
public interface ElasticsearchFieldConverter {

	/**
	 * @param value A value passed through the predicate or sort DSL.
	 * @param context
	 * @return A value of the type used internally when querying this field.
	 * @throws RuntimeException If the value does not match the expected type.
	 */
	JsonElement convertFromDsl(Object value,
			ToIndexFieldValueConvertContext context);

	/**
	 * @param value The projected value returned by the codec.
	 * @param context The context to use when converting.
	 * @return A value of the type expected by users when projecting.
	 */
	Object convertFromProjection(JsonElement value, FromIndexFieldValueConvertContext context);

	/**
	 * Determine whether another converter's {@link #convertFromDsl(Object, ToIndexFieldValueConvertContext)}
	 * method is compatible with this one's.
	 * <p>
	 * @see org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter#isConvertFromDslCompatibleWith(UserIndexFieldConverter)
	 *
	 * @param other Another {@link ElasticsearchFieldConverter}, never {@code null}.
	 * @return {@code true} if the given converter's
	 * {@link #convertFromDsl(Object, ToIndexFieldValueConvertContext)} method is compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isConvertFromDslCompatibleWith(ElasticsearchFieldConverter other);

	/**
	 * Determine whether another converter's {@link #convertFromProjection(JsonElement, FromIndexFieldValueConvertContext)}
	 * method is compatible with this one's.
	 * <p>
	 * @see org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter#isConvertFromProjectionCompatibleWith(UserIndexFieldConverter)
	 *
	 * @param other Another {@link ElasticsearchFieldConverter}, never {@code null}.
	 * @return {@code true} if the given converter's
	 * {@link #convertFromProjection(JsonElement, FromIndexFieldValueConvertContext)} method is compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isConvertFromProjectionCompatibleWith(ElasticsearchFieldConverter other);

	/**
	 * Determine whether the given projection type is compatible with this converter.
	 *
	 * @param projectionType The projection type.
	 * @return {@code true} if the given projection type is compatible. {@code false} otherwise.
	 */
	boolean isProjectionCompatibleWith(Class<?> projectionType);
}
