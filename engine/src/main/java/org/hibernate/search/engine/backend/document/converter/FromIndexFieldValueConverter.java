/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter;

/**
 * A converter from a source index field value to a different value.
 *
 * @param <F> The type of source, index field values.
 * @param <V> The type of target values.
 */
public interface FromIndexFieldValueConverter<F, V> {

	/**
	 * @return The type of the converted value.
	 */
	Class<?> getConvertedType();

	/**
	 * @param value The index field value to convert.
	 * @return The converted value.
	 */
	V convert(F value);

}
