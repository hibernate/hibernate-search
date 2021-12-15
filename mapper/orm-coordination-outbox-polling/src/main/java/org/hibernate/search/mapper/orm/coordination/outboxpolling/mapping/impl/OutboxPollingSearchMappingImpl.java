/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.impl;

import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.OutboxPollingSearchMapping;

public class OutboxPollingSearchMappingImpl implements OutboxPollingSearchMapping {

	private final TransactionHelper transactionHelper;
	private final SessionFactoryImplementor sessionFactory;
	private final Set<String> tenantIds;

	public OutboxPollingSearchMappingImpl(CoordinationStrategyStartContext context, Set<String> tenantIds) {
		this.sessionFactory = context.mapping().sessionFactory();
		this.transactionHelper = new TransactionHelper( sessionFactory );
		this.tenantIds = tenantIds;
	}

	@Override
	public int countAbortedEvents() {
		// TODO HSEARCH-4283 Implement it
		return 0;
	}

	@Override
	public void reprocessAbortedEvents() {
		// TODO HSEARCH-4283 Implement it
	}

	@Override
	public void clearAllAbortedEvents() {
		// TODO HSEARCH-4283 Implement it
	}
}
