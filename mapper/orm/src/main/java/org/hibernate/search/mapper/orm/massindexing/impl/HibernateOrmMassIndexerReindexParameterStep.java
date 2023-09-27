/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
