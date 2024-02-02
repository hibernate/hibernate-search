/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

/**
 * Context exposed to {@link PojoMassLoadingStrategy}.
 * <p>
 * Mappers will generally need to cast this type to the mapper-specific subtype.
 * @see PojoMassIdentifierLoadingContext#parent()
 * @see PojoMassEntityLoadingContext#parent()
 * @see PojoMassLoadingStrategy#groupingAllowed(PojoLoadingTypeContext, PojoMassLoadingContext)
 */
public interface PojoMassLoadingContext {

}
