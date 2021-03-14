/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import org.hibernate.CacheMode;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingOptions;

public interface HibernateOrmMassIndexingOptions extends MassIndexingOptions {

	/**
	 * @return the transaction timeout
	 */
	Integer transactionTimeout();

	/**
	 * @return the {@link CacheMode}
	 */
	CacheMode cacheMode();
}
