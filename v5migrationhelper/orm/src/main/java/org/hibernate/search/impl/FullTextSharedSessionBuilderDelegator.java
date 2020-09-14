/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.impl;

import java.sql.Connection;
import java.util.TimeZone;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextSharedSessionBuilder;
import org.hibernate.search.Search;

/**
 * @author Emmanuel Bernard
 */
class FullTextSharedSessionBuilderDelegator implements FullTextSharedSessionBuilder {

	private final SharedSessionBuilder builder;

	public FullTextSharedSessionBuilderDelegator(SharedSessionBuilder builder) {
		this.builder = builder;
	}

	@Override
	public FullTextSharedSessionBuilder interceptor() {
		builder.interceptor();
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder connection() {
		builder.connection();
		return this;
	}

	@Deprecated
	@Override
	public FullTextSharedSessionBuilder connectionReleaseMode() {
		builder.connectionReleaseMode();
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder autoJoinTransactions() {
		builder.autoJoinTransactions();
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder autoClose() {
		builder.autoClose();
		return this;
	}

	@Deprecated
	@Override
	public FullTextSharedSessionBuilder flushBeforeCompletion() {
		builder.flushBeforeCompletion();
		return this;
	}

	@Deprecated
	@Override
	public FullTextSharedSessionBuilder transactionContext() {
		builder.transactionContext();
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder interceptor(Interceptor interceptor) {
		builder.interceptor( interceptor );
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder noInterceptor() {
		builder.noInterceptor();
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder connection(Connection connection) {
		builder.connection( connection );
		return this;
	}

	@Deprecated
	@Override
	public FullTextSharedSessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		builder.connectionReleaseMode( connectionReleaseMode );
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
		builder.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Deprecated
	@Override
	public FullTextSharedSessionBuilder autoClose(boolean autoClose) {
		builder.autoClose( autoClose );
		return this;
	}

	@Deprecated
	@Override
	public FullTextSharedSessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion) {
		builder.flushBeforeCompletion( flushBeforeCompletion );
		return this;
	}

	@Override
	public FullTextSession openSession() {
		return Search.getFullTextSession( builder.openSession() );
	}

	@Override
	public FullTextSharedSessionBuilder tenantIdentifier(String tenantIdentifier) {
		builder.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder clearEventListeners() {
		builder.clearEventListeners();
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder eventListeners(SessionEventListener... listeners) {
		builder.eventListeners( listeners );
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder statementInspector(StatementInspector statementInspector) {
		builder.statementInspector( statementInspector );
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder connectionHandlingMode() {
		builder.connectionHandlingMode();
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder flushMode() {
		builder.flushMode();
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder connectionHandlingMode(PhysicalConnectionHandlingMode mode) {
		builder.connectionHandlingMode( mode );
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder autoClear(boolean autoClear) {
		builder.autoClear( autoClear );
		return this;
	}

	@Override
	public FullTextSharedSessionBuilder flushMode(FlushMode flushMode) {
		builder.flushMode( flushMode );
		return this;
	}

	@Override
	public SessionBuilder jdbcTimeZone(TimeZone timeZone) {
		builder.jdbcTimeZone( timeZone );
		return this;
	}

}
