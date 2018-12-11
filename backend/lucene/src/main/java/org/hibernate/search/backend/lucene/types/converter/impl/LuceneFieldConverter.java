/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserDocumentFieldConverter;

/**
 * Defines how a given value will be converted when performing search queries.
 * <p>
 * Used by predicate and sort builders in particular, and also by hit extractors when projecting in a search query.
 *
 * @param <F> The type of the index field.
 * @param <T> The type used internally when querying. May be different from the field type exposed to users;
 * see for example {@link LuceneLocalDateFieldConverter}.
 */
public interface LuceneFieldConverter<F, T> {

	/**
	 * @param value A value passed through the predicate or sort DSL.
	 * @param context The context to use when converting.
	 * @return A value of the type used internally when querying this field.
	 * @throws RuntimeException If the value does not match the expected type.
	 */
	T convertDslToIndex(Object value, ToDocumentFieldValueConvertContext context);

	/**
	 * @param indexValue The projected value returned by the codec.
	 * @param context The context to use when converting.
	 * @return A value of the type expected by users when projecting.
	 */
	Object convertIndexToProjection(F indexValue, FromDocumentFieldValueConvertContext context);

	/**
	 * Determine whether another converter's {@link #convertDslToIndex(Object, ToDocumentFieldValueConvertContext)}
	 * method is compatible with this one's.
	 * <p>
	 * @see UserDocumentFieldConverter#isConvertDslToIndexCompatibleWith(UserDocumentFieldConverter)
	 *
	 * @param other Another {@link LuceneFieldConverter}, never {@code null}.
	 * @return {@code true} if the given converter's
	 * {@link #convertDslToIndex(Object, ToDocumentFieldValueConvertContext)} method is compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isConvertDslToIndexCompatibleWith(LuceneFieldConverter<?, ?> other);

	/**
	 * Determine whether another converter's {@link #convertIndexToProjection(Object, FromDocumentFieldValueConvertContext)}
	 * method is compatible with this one's.
	 * <p>
	 * @see UserDocumentFieldConverter#isConvertIndexToProjectionCompatibleWith(UserDocumentFieldConverter)
	 *
	 * @param other Another {@link LuceneFieldConverter}, never {@code null}.
	 * @return {@code true} if the given converter's
	 * {@link #convertIndexToProjection(Object, FromDocumentFieldValueConvertContext)} method is compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isConvertIndexToProjectionCompatibleWith(LuceneFieldConverter<?, ?> other);

	/**
	 * Determine whether the given projection type is compatible with this converter.
	 *
	 * @param projectionType The projection type.
	 * @return {@code true} if the given projection type is compatible. {@code false} otherwise.
	 */
	boolean isProjectionCompatibleWith(Class<?> projectionType);

}
