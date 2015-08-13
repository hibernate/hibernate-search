/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.cfg.spi;

import org.hibernate.search.engine.service.spi.Service;

/**
 * Contract checking that two given classes cannot share the same identifier and be two different
 * instances in the underlying datastore (or event provider).
 *
 * In the case of Hibernate ORM, the two classes would enforce id uniqueness
 * when they share the same mapped class hierarchy as ORM enforces one id definition per class hierarchy
 * amongst mapped classes.
 *
 * This {@link org.hibernate.search.engine.service.spi.Service} can be provided by the
 * {@link org.hibernate.search.cfg.spi.SearchConfiguration} implementor when it has such knowledge.
 * If no {@code IdUniquenessResolver} is enlisted as provided service, then it is assumed
 * that uniqueness cannot be guaranteed.
 *
 * This contract is used by Hibernate Search to decide whether it can optimize delete operations
 * on a given index or not.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface IdUniquenessResolver extends Service {
	/**
	 * Returns {@code true} if the same identifier value cannot be share between
	 * two class instances of {@code entityInIndex} and {@code otherEntityInIndex}.
	 * @param entityInIndex one entity class
	 * @param otherEntityInIndex the other entity class
	 * @return {@code true} if the same identifier value cannot be share between
	 * two class instances of {@code entityInIndex} and {@code otherEntityInIndex}
	 */
	boolean areIdsUniqueForClasses(Class<?> entityInIndex, Class<?> otherEntityInIndex);
}
