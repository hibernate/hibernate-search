/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import java.util.Set;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * The context passed to {@link PojoMassIndexingLoadingStrategy#createIdentifierLoader(PojoMassIndexingIdentifierLoadingContext)}.
 *
 * @param <E> The type of loaded entities.
 * @param <I> The type of entity identifiers.
 */
public interface PojoMassIndexingIdentifierLoadingContext<E, I> {

	/**
	 * @return The identifiers of the types of all entities that will be loaded by the entity loader.
	 */
	Set<PojoRawTypeIdentifier<? extends E>> includedTypes();

	/**
	 * @return A sink that the loader will add loaded entities to.
	 */
	PojoMassIdentifierSink<I> createSink();

	/**
	 * @return The tenant identifier to use ({@code null} if none).
	 */
	String tenantIdentifier();
}
