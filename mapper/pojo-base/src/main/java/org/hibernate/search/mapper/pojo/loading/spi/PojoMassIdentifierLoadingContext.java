/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

/**
 * The context passed to {@link PojoMassLoadingStrategy#createIdentifierLoader(java.util.Set, PojoMassIdentifierLoadingContext)}.
 *
 * @param <I> The type of entity identifiers.
 */
public interface PojoMassIdentifierLoadingContext<I> {

	/**
	 * @return The parent, mapper-specific loading context.
	 */
	PojoMassLoadingContext parent();

	/**
	 * @return A sink that the loader will add loaded entities to.
	 */
	PojoMassIdentifierSink<I> createSink();

	/**
	 * @return The tenant identifier to use ({@code null} if none).
	 */
	String tenantIdentifier();

}
