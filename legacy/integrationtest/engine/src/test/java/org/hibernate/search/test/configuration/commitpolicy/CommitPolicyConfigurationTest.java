/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.commitpolicy;

import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.PerChangeSetCommitPolicy;
import org.hibernate.search.backend.impl.lucene.ScheduledCommitPolicy;
import org.hibernate.search.backend.impl.lucene.SharedIndexCommitPolicy;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.configuration.mutablefactory.A;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hibernate.search.backend.impl.lucene.ScheduledCommitPolicy.DEFAULT_DELAY_MS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Configuration tests for commit policies
 *
 * @author gustavonalle
 */
@Category(SkipOnElasticsearch.class) // This test is specific to Lucene
public class CommitPolicyConfigurationTest {

	private final IndexedTypeIdentifier testType = PojoIndexedTypeIdentifier.convertFromLegacy( A.class );

	@Rule
	public SearchFactoryHolder sfSyncExclusiveIndex = new SearchFactoryHolder( A.class )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" );

	@Rule
	public SearchFactoryHolder sfSyncSharedIndex = new SearchFactoryHolder( A.class )
			.withProperty( "hibernate.search.default.exclusive_index_use", "false" );

	@Rule
	public SearchFactoryHolder sfAsyncSharedIndex = new SearchFactoryHolder( A.class )
			.withProperty( "hibernate.search.default.default.worker.execution", "async" )
			.withProperty( "hibernate.search.default.exclusive_index_use", "false" );

	@Rule
	public SearchFactoryHolder sfAsyncExclusiveIndex = new SearchFactoryHolder( A.class )
			.withProperty( "hibernate.search.default.worker.execution", "async" )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" );

	@Rule
	public SearchFactoryHolder sfAsyncExclusiveIndexCustomPeriod = new SearchFactoryHolder( A.class )
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
		AbstractWorkspaceImpl workspace = sfHolder.extractWorkspace( testType );
		return workspace.getCommitPolicy();
	}

}
