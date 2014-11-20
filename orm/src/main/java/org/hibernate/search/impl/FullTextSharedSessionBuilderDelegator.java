/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.impl;

import java.sql.Connection;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextSharedSessionBuilder;
import org.hibernate.search.Search;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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

	@Override
	public FullTextSharedSessionBuilder flushBeforeCompletion() {
		builder.flushBeforeCompletion();
		return this;
	}

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

	@Override
	public FullTextSharedSessionBuilder autoClose(boolean autoClose) {
		builder.autoClose( autoClose );
		return this;
	}

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
}
