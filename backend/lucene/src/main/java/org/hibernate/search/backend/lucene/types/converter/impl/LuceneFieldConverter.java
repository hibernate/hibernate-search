/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

/**
 * Defines how a given value will be converted when performing search queries.
 * <p>
 * Used by predicate and sort builders in particular.
 *
 * @param <T> The type used internally when querying. May be different from the field type exposed to users;
 * see for example {@link LocalDateFieldConverter}.
 */
public interface LuceneFieldConverter<T> {

	/**
	 * @param value A value passed through the predicate or sort DSL.
	 * @return A value of the type used internally when querying this field.
	 */
	T convertFromDsl(Object value);

	// equals()/hashCode() needs to be implemented if the converter is not a singleton

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();
}
