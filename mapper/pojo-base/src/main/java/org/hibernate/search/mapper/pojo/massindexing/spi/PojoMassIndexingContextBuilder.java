/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

/**
 * A builder for {@link PojoMassIndexingContext},
 * allowing changes to the parameters of object loading,
 * for example while a query is being built.
 *
 * @param <LOS> The type of the initial step of the mass indexing loading options definition DSL
 */
public interface PojoMassIndexingContextBuilder<LOS> {

	/**
	 * @return The inital step of the mass indexing loading options definition DSL passed to user-defined consumers added through
	 */
	LOS toAPI();

	/**
	 * @return The configured loading context.
	 */
	PojoMassIndexingContext<?> build();
}
