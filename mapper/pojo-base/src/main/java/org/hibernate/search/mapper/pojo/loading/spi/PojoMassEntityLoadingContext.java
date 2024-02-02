/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.Set;

import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

/**
 * The context passed to {@link PojoMassLoadingStrategy#createEntityLoader(Set, PojoMassEntityLoadingContext)}.
 *
 * @param <E> The type of loaded entities.
 */
public interface PojoMassEntityLoadingContext<E> {

	/**
	 * @return The parent, mapper-specific loading context.
	 */
	PojoMassLoadingContext parent();

	/**
	 * @param sessionContext The session context, used to create an indexer in particular.
	 * @return A sink that the loader will add loaded entities to.
	 */
	PojoMassEntitySink<E> createSink(PojoMassIndexingSessionContext sessionContext);

	/**
	 * @return The tenant identifier to use ({@code null} if none).
	 */
	String tenantIdentifier();

}
