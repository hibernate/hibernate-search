/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchReindexCondition;

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
	 * @param paramNamePrefix A unique prefix for the name of parameters added by the resulting expression.
	 * @param idObj The ID all results should be lesser than.
	 * @param inclusive Whether the {@code idObj} should also be included.
	 * @return A "strictly greater than" restriction on the ID.
	 */
	HibernateOrmBatchReindexCondition idGreater(String paramNamePrefix, Object idObj, boolean inclusive);

	/**
	 * @param paramNamePrefix A unique prefix for the name of parameters added by the resulting expression.
	 * @param idObj The ID all results should be lesser than.
	 * @param inclusive Whether the {@code idObj} should also be included.
	 * @return A "lesser than" restriction on the ID.
	 */
	HibernateOrmBatchReindexCondition idLesser(String paramNamePrefix, Object idObj, boolean inclusive);

	String ascOrder();

}
