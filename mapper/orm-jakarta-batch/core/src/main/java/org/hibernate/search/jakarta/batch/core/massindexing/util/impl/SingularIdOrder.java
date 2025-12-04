/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import java.util.Map;

import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchReindexCondition;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingTypeContext;

/**
 * Order over a single ID attribute.
 * <p>
 * This class should be used when target entity has a single ID attribute.
 *
 * @author Mincong Huang
 * @author Yoann Rodiere
 */
public class SingularIdOrder<E> implements IdOrder {

	private final String idPropertyName;

	public SingularIdOrder(HibernateOrmLoadingTypeContext<E> type) {
		this.idPropertyName = type.entityMappingType().getIdentifierMapping().getAttributeName();
	}

	@Override
	public HibernateOrmBatchReindexCondition idGreater(String paramNamePrefix, Object idObj, boolean inclusive) {
		return restrict( paramNamePrefix, inclusive ? ">=" : ">", idObj );
	}

	@Override
	public HibernateOrmBatchReindexCondition idLesser(String paramNamePrefix, Object idObj, boolean inclusive) {
		return restrict( paramNamePrefix, inclusive ? "<=" : "<", idObj );
	}

	@Override
	public String ascOrder() {
		return idPropertyName + " asc";
	}

	private HibernateOrmBatchReindexCondition restrict(String paramNamePrefix, String operator, Object idObj) {
		String paramName = paramNamePrefix + "REF";
		return new BatchCoreHqlReindexCondition(
				idPropertyName + " " + operator + " :" + paramName,
				Map.of( paramName, idObj )
		);
	}

}
