/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <E> The entity type mapped to the index.
 */
public interface PojoMassIndexingIndexedTypeContext<E> extends PojoLoadingTypeContext<E> {

	Supplier<E> toEntitySupplier(PojoWorkSessionContext sessionContext, Object entity);

	IdentifierMappingImplementor<?, E> identifierMapping();

}
