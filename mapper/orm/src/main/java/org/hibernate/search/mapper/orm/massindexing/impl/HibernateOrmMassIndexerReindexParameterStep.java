/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerReindexParameterStep;

public class HibernateOrmMassIndexerReindexParameterStep extends HibernateOrmMassIndexerFilteringTypeStep
		implements MassIndexerReindexParameterStep {

	private final ConditionalExpression expression;

	public HibernateOrmMassIndexerReindexParameterStep(HibernateOrmMassIndexer massIndexer, Class<?> type,
			ConditionalExpression expression) {
		super( massIndexer, type );
		this.expression = expression;
	}

	@Override
	public MassIndexerReindexParameterStep param(String name, Object value) {
		expression.param( name, value );
		return this;
	}
}
