/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
public class FullTextSharedSessionBuilderDelegator implements FullTextSharedSessionBuilder {
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
