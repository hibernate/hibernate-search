/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.converter.impl;

import org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

import com.google.gson.JsonElement;

/**
 * Defines how a given value will be converted when performing search queries.
 * <p>
 * Used by predicate and sort builders in particular, and also by hit extractors when projecting in a search query.
 */
public interface ElasticsearchFieldConverter {

	/**
	 * @param value A value passed through the predicate or sort DSL.
	 * @return A value of the type used internally when querying this field.
	 * @throws RuntimeException If the value does not match the expected type.
	 */
	JsonElement convertFromDsl(Object value);

	/**
	 * @param value The projected value returned by the codec.
	 * @param sessionContext The session context provided by the mapper.
	 * @return A value of the type expected by users when projecting.
	 */
	Object convertFromProjection(JsonElement value, SessionContextImplementor sessionContext);

	/**
	 * Determine whether another converter is DSL-compatible with this one.
	 *
	 * @see org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter#isDslCompatibleWith(UserIndexFieldConverter)
	 *
	 * @param other Another {@link ElasticsearchFieldConverter}, never {@code null}.
	 * @return {@code true} if the given converter is DSL-compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isDslCompatibleWith(ElasticsearchFieldConverter other);

	/**
	 * Determine whether the given projection type is compatible with this converter.
	 *
	 * @param projectionType The projection type.
	 * @return {@code true} if the given projection type is compatible. {@code false} otherwise
	 */
	boolean isProjectionCompatibleWith(Class<?> projectionType);
}
