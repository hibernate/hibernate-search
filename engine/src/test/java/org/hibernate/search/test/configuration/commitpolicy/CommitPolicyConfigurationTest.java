/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration.commitpolicy;

import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.PerChangeSetCommitPolicy;
import org.hibernate.search.backend.impl.lucene.ScheduledCommitPolicy;
import org.hibernate.search.backend.impl.lucene.SharedIndexCommitPolicy;
import org.hibernate.search.test.backend.lucene.Quote;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.junit.Rule;
import org.junit.Test;

import static org.hibernate.search.backend.impl.lucene.ScheduledCommitPolicy.DEFAULT_DELAY_MS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Configuration tests for commit policies
 *
 * @author gustavonalle
 */
public class CommitPolicyConfigurationTest {

	@Rule
	public SearchFactoryHolder sfSyncExclusiveIndex = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" );

	@Rule
	public SearchFactoryHolder sfSyncSharedIndex = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.exclusive_index_use", "false" );

	@Rule
	public SearchFactoryHolder sfAsyncSharedIndex = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.default.worker.execution", "async" )
			.withProperty( "hibernate.search.default.exclusive_index_use", "false" );

	@Rule
	public SearchFactoryHolder sfAsyncExclusiveIndex = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.worker.execution", "async" )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" );

	@Rule
	public SearchFactoryHolder sfAsyncExclusiveIndexCustomPeriod = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.index_flush_interval", "100" )
			.withProperty( "hibernate.search.default.worker.execution", "async" )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" );

	@Test
	public void testConfiguredCommitPolicy() throws Exception {
		assertCommitPolicyUsed( sfSyncExclusiveIndex, PerChangeSetCommitPolicy.class );
		assertCommitPolicyUsed( sfSyncSharedIndex, SharedIndexCommitPolicy.class );
		assertCommitPolicyUsed( sfAsyncSharedIndex, SharedIndexCommitPolicy.class );
		assertCommitPolicyUsed( sfAsyncExclusiveIndex, ScheduledCommitPolicy.class );
	}

	@Test
	public void testDefaultValue() throws Exception {
		assertEquals( 100, extractInterval( sfAsyncExclusiveIndexCustomPeriod ) );
		assertEquals( DEFAULT_DELAY_MS, extractInterval( sfAsyncExclusiveIndex ) );
	}

	private void assertCommitPolicyUsed(SearchFactoryHolder sfHolder, Class<? extends CommitPolicy> commitPolicyClass) {
		CommitPolicy commitPolicy = getCommitPolicy( sfHolder );
		assertTrue( commitPolicyClass.isAssignableFrom( commitPolicy.getClass() ) );
	}

	private int extractInterval(SearchFactoryHolder sfHolder) {
		ScheduledCommitPolicy commitPolicy = (ScheduledCommitPolicy) getCommitPolicy( sfHolder );
		return commitPolicy.getDelay();
	}

	private CommitPolicy getCommitPolicy(SearchFactoryHolder sfHolder) {
		AbstractWorkspaceImpl workspace = sfHolder.extractWorkspace( Quote.class );
		return workspace.getCommitPolicy();
	}

}
