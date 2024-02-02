/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Optional;

import org.hibernate.CacheMode;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.loading.spi.LoadingMappingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;

public interface HibernateOrmMassLoadingContext extends PojoMassIndexingContext {

	LoadingMappingContext mapping();

	/**
	 * @return the transaction timeout
	 */
	Integer idLoadingTransactionTimeout();

	/**
	 * @return the {@link CacheMode}
	 */
	CacheMode cacheMode();

	/**
	 * @return how many entities to load and index in each batch.
	 */
	int objectLoadingBatchSize();

	/**
	 * @return the objects limit used to load the root entities.
	 */
	long objectsLimit();

	/**
	 * @return fetch size used to load the root entities.
	 */
	int idFetchSize();

	/**
	 * @return The conditional expression to apply when loading the given type,
	 * inherited from supertypes by default,
	 * or {@link Optional#empty()} if there is no condition to apply.
	 */
	Optional<ConditionalExpression> conditionalExpression(PojoLoadingTypeContext<?> typeContext);
}
