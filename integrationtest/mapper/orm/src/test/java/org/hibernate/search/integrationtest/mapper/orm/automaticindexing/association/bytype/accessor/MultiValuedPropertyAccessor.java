/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;

public interface MultiValuedPropertyAccessor<R, V, C> extends PropertyAccessor<R, V> {

	static <R, V, C> MultiValuedPropertyAccessor<R, V, C> create(ContainerPrimitives<C, V> containerPrimitives,
			Function<R, C> getContainerMethod) {
		return new SimpleMultiValuedPropertyAccessor<>( containerPrimitives, getContainerMethod );
	}

	static <R, V, C> MultiValuedPropertyAccessor<R, V, C> create(ContainerPrimitives<C, V> containerPrimitives,
			Function<R, C> getContainerMethod,
			BiConsumer<R, C> setContainerMethod) {
		return new SimpleMultiValuedPropertyAccessor<>( containerPrimitives, getContainerMethod, setContainerMethod );
	}

	void add(R root, V value);

	void remove(R root, V value);

	void setContainer(R root, C container);

	C getContainer(R root);

}
