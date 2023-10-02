/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
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
	public ConditionalExpression idGreater(String paramNamePrefix, Object idObj) {
		return restrict( paramNamePrefix, ">", idObj );
	}

	@Override
	public ConditionalExpression idGreaterOrEqual(String paramNamePrefix, Object idObj) {
		return restrict( paramNamePrefix, ">=", idObj );
	}

	@Override
	public ConditionalExpression idLesser(String paramNamePrefix, Object idObj) {
		return restrict( paramNamePrefix, "<", idObj );
	}

	@Override
	public String ascOrder() {
		return idPropertyName + " asc";
	}

	private ConditionalExpression restrict(String paramNamePrefix, String operator, Object idObj) {
		String paramName = paramNamePrefix + "REF";
		var expression = new ConditionalExpression( idPropertyName + " " + operator + " :" + paramName );
		expression.param( paramName, idObj );
		return expression;
	}

}
