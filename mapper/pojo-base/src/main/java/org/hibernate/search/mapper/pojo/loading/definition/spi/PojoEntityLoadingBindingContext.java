/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.definition.spi;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;

public interface PojoEntityLoadingBindingContext {

	/**
	 * @param expectedEntitySuperType An expected entity supertype that the strategy can handle.
	 * @param strategy The strategy for selection loading, used in particular during search.
	 * @param <E> An expected entity supertype that the strategy can handle.
	 */
	<E> void selectionLoadingStrategy(Class<E> expectedEntitySuperType, PojoSelectionLoadingStrategy<? super E> strategy);

	/**
	 * @param expectedEntitySuperType An expected entity supertype that the strategy can handle.
	 * @param strategy The strategy for mass loading, used in particular during mass indexing.
	 * @param <E> An expected entity supertype that the strategy can handle.
	 */
	<E> void massLoadingStrategy(Class<E> expectedEntitySuperType, PojoMassLoadingStrategy<? super E, ?> strategy);

}
