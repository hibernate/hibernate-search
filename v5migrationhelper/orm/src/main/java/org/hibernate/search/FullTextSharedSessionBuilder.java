/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import java.sql.Connection;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.SharedSessionBuilder;

/**
 * @author Emmanuel Bernard
 */
public interface FullTextSharedSessionBuilder extends SharedSessionBuilder {
	@Override
	FullTextSharedSessionBuilder interceptor();

	@Override
	FullTextSharedSessionBuilder connection();

	@Deprecated
	@Override
	FullTextSharedSessionBuilder connectionReleaseMode();

	@Override
	FullTextSharedSessionBuilder autoJoinTransactions();

	@Override
	FullTextSharedSessionBuilder autoClose();

	@Deprecated
	@Override
	FullTextSharedSessionBuilder flushBeforeCompletion();

	@Deprecated
	@Override
	FullTextSharedSessionBuilder transactionContext();

	@Override
	FullTextSharedSessionBuilder interceptor(Interceptor interceptor);

	@Override
	FullTextSharedSessionBuilder noInterceptor();

	@Override
	FullTextSharedSessionBuilder connection(Connection connection);

	@Deprecated
	@Override
	FullTextSharedSessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

	@Override
	FullTextSharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	@Deprecated
	@Override
	FullTextSharedSessionBuilder autoClose(boolean autoClose);

	@Deprecated
	@Override
	FullTextSharedSessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion);

	@Override
	FullTextSession openSession();

	@Override
	FullTextSharedSessionBuilder tenantIdentifier(String tenantIdentifier);
}
