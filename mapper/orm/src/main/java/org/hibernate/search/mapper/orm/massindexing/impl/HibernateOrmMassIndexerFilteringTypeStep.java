/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerFilteringTypeStep;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerReindexParameterStep;

public class HibernateOrmMassIndexerFilteringTypeStep implements MassIndexerFilteringTypeStep {

	private final HibernateOrmMassIndexer massIndexer;
	private final Class<?> type;

	public HibernateOrmMassIndexerFilteringTypeStep(HibernateOrmMassIndexer massIndexer, Class<?> type) {
		this.massIndexer = massIndexer;
		this.type = type;
	}

	@Override
	public MassIndexerReindexParameterStep reindexOnly(String conditionalExpression) {
		ConditionalExpression expression = massIndexer.reindexOnly( type, conditionalExpression );
		return new HibernateOrmMassIndexerReindexParameterStep( massIndexer, type, expression );
	}
}
