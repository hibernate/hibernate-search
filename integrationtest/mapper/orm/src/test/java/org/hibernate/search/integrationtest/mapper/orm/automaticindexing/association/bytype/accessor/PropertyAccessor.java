/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface PropertyAccessor<R, V> {

	static <R, V> SingleValuedPropertyAccessor<R, V> create(BiConsumer<R, V> setMethod) {
		return new SingleValuedPropertyAccessor<>( setMethod );
	}

	static <R, V> SingleValuedPropertyAccessor<R, V> create(BiConsumer<R, V> setMethod,
			Function<R, V> getMethod) {
		return new SingleValuedPropertyAccessor<>( setMethod, getMethod );
	}

	void set(R root, V value);

	V get(R root);

	void clear(R root);

}
