/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.util.function.Consumer;

import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerFilteringTypeStep;

public class HibernateOrmMassIndexerFilteringTypeStep implements MassIndexerFilteringTypeStep {

	private final HibernateOrmMassIndexer massIndexer;
	private final Class<?> type;

	public HibernateOrmMassIndexerFilteringTypeStep(HibernateOrmMassIndexer massIndexer, Class<?> type) {
		this.massIndexer = massIndexer;
		this.type = type;
	}

	@Override
	public MassIndexerFilteringTypeStep reindexOnly(String conditionalExpression) {
		massIndexer.reindexOnly( type, conditionalExpression );
		return this;
	}

	@Override
	public MassIndexerFilteringTypeStep reindexOnly(String conditionalExpression, Consumer<Query<?>> queryConsumer) {
		ConditionalExpression expression = massIndexer.reindexOnly( type, conditionalExpression );
		expression.defineQueryConsumer( queryConsumer );
		return this;
	}
}
