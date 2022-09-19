/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.util.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Provides ID-based, order-sensitive restrictions
 * and ascending ID order for the indexed type,
 * allowing to easily build partitions based on ID order.
 *
 * @author Mincong Huang
 * @author Yoann Rodiere
 */
public interface IdOrder {

	/**
	 * @param idObj The ID all results should be greater than.
	 * @return A "greater than" restriction on the ID.
	 */
	Predicate idGreater(CriteriaBuilder builder, Root<?> root, Object idObj);

	/**
	 * @param idObj The ID all results should be greater than or equal to.
	 * @return A "greater or equal" restriction on the ID.
	 */
	Predicate idGreaterOrEqual(CriteriaBuilder builder, Root<?> root, Object idObj);

	/**
	 * @param idObj The ID all results should be lesser than.
	 * @return A "lesser than" restriction on the ID.
	 */
	Predicate idLesser(CriteriaBuilder builder, Root<?> root, Object idObj);

	void addAscOrder(CriteriaBuilder builder, CriteriaQuery<?> criteria, Root<?> root);

}
