/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.schema.management;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An extension to the index schema export, allowing to access backend-specific methods of a schema export.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended index schema export.
 * Should generally extend {@link SchemaExport}.
 * @see SchemaExport#extension(SchemaExportExtension)
 */
@Incubating
public interface SchemaExportExtension<T> {

	/**
	 * Attempt to extend a given export, throwing an exception in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link SchemaExport}.
	 * @return An extended {@link T index schema export}.
	 */
	T extendOrFail(SchemaExport original);
}
