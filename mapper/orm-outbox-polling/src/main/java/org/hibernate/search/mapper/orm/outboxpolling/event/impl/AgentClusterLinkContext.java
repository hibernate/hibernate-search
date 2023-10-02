/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.common.spi.SessionHelper;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public class AgentClusterLinkContext {

	private final TransactionHelper transactionHelper;
	private final SessionHelper sessionHelper;
	private final AgentRepositoryProvider agentRepositoryProvider;

	private SessionImplementor session;
	private AgentRepository agentRepository;

	public AgentClusterLinkContext(TransactionHelper transactionHelper, SessionHelper sessionHelper,
			AgentRepositoryProvider agentRepositoryProvider) {
		this.transactionHelper = transactionHelper;
		this.sessionHelper = sessionHelper;
		this.agentRepositoryProvider = agentRepositoryProvider;
	}

	void begin() {
		session = sessionHelper.openSession();
		transactionHelper.begin( session );
		agentRepository = agentRepositoryProvider.create( session );
	}

	public AgentRepository agentRepository() {
		return agentRepository;
	}

	public void commitAndBeginNewTransaction() {
		commit();
		begin();
	}

	void commit() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( transactionHelper::commit, session );
			closer.push( SessionImplementor::close, session );
			session = null;
			agentRepository = null;
		}
	}

	void rollbackLatestTransactionSafely(Throwable t) {
		new SuppressingCloser( t )
				.push( s -> transactionHelper.rollbackSafely( s, t ), session )
				.push( SessionImplementor::close, session );
	}

}
