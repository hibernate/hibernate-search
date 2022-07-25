/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.mapper.orm.common.spi.SessionHelper;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepositoryProvider;

public class AgentClusterLinkContextProvider {

	private final TransactionHelper transactionHelper;
	private final SessionHelper sessionHelper;
	private final AgentRepositoryProvider agentRepositoryProvider;

	public AgentClusterLinkContextProvider(TransactionHelper transactionHelper, SessionHelper sessionHelper,
			AgentRepositoryProvider agentRepositoryProvider) {
		this.transactionHelper = transactionHelper;
		this.sessionHelper = sessionHelper;
		this.agentRepositoryProvider = agentRepositoryProvider;
	}

	public final void inTransaction(Consumer<AgentClusterLinkContext> action) {
		inTransaction( context -> {
			action.accept( context );
			return null;
		} );
	}

	public <T> T inTransaction(Function<AgentClusterLinkContext, T> action) {
		AgentClusterLinkContext context = new AgentClusterLinkContext( transactionHelper, sessionHelper,
				agentRepositoryProvider );
		T result;
		try {
			context.begin();
			result = action.apply( context );
		}
		catch (Throwable t) {
			context.rollbackLatestTransactionSafely( t );
			throw t;
		}
		context.commit();
		return result;
	}

}
