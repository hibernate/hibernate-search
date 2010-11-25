/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.infinispan;

import java.util.List;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.apache.lucene.search.Query;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.test.util.JGroupsEnvironment;

/**
 * We start two different Hibernate Search instances, both using
 * an InfinispanDirectoryProvider as the default DirectoryProvider
 * for all entities.
 * Set -Djava.net.preferIPv4Stack=true as this is required by JGroups.
 *
 * @author Sanne Grinovero
 */
public class TwoNodesTest extends TestCase {

	final FullTextSessionBuilder nodea = new FullTextSessionBuilder();
	final FullTextSessionBuilder nodeb = new FullTextSessionBuilder();

	public void testSomething() {
		final String to = "spam@hibernate.org";
		final String messageText = "to get started as a real spam expert, search for 'getting an iphone' on Hibernate forums";
		{
			FullTextSession fullTextSession = nodea.openFullTextSession();
			Transaction transaction = fullTextSession.beginTransaction();
			SimpleEmail mail = new SimpleEmail();
			mail.to = to;
			mail.message = messageText;
			fullTextSession.save( mail );
			transaction.commit();
		}
		{
			FullTextSession fullTextSession = nodeb.openFullTextSession();
			Transaction transaction = fullTextSession.beginTransaction();
			QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
					.buildQueryBuilder()
					.forEntity( SimpleEmail.class )
					.get();
			Query query = queryBuilder.keyword()
					.onField( "message" )
					.matching( "Hibernate Getting Started" )
					.createQuery();
			List list = fullTextSession.createFullTextQuery( query ).setProjection( "message" ).list();
			Assert.assertEquals( 1, list.size() );
			Object[] result = (Object[]) list.get( 0 );
			Assert.assertEquals( messageText, result[0] );
			transaction.commit();
		}
	}

	@Override
	protected void setUp() throws Exception {
		JGroupsEnvironment.initJGroupsProperties();
		prepareCommonConfiguration( nodea );
		nodea.build();
		prepareCommonConfiguration( nodeb );
		nodeb.build();

		InfinispanDirectoryProvider directoryProviderA = extractInfinispanDirectoryProvider( nodea );
		EmbeddedCacheManager cacheManager = directoryProviderA.getCacheManager();
		waitMembersCount( cacheManager, 2 );
	}

	/**
	 * Wait some time for the cluster to form
	 *
	 * @param cacheManager
	 * @param expectedSize
	 *
	 * @throws InterruptedException
	 */
	private void waitMembersCount(EmbeddedCacheManager cacheManager, int expectedSize) throws InterruptedException {
		int currentSize = 0;
		int loopCounter = 0;
		while ( currentSize < expectedSize ) {
			Thread.sleep( 10 );
			List<Address> members = cacheManager.getMembers();
			currentSize = members.size();
			if ( loopCounter > 200 ) {
				throw new AssertionFailedError( "timeout while waiting for all nodes to join in cluster" );
			}
		}
	}

	public InfinispanDirectoryProvider extractInfinispanDirectoryProvider(FullTextSessionBuilder sessionBuilder) {
		SearchFactory searchFactory = sessionBuilder.getSearchFactory();
		DirectoryProvider[] directoryProviders = searchFactory.getDirectoryProviders( SimpleEmail.class );
		return (InfinispanDirectoryProvider) directoryProviders[0];
	}

	private void prepareCommonConfiguration(FullTextSessionBuilder cfg) {
		cfg.setProperty( "hibernate.search.default.directory_provider", "infinispan" );
		cfg.setProperty(
				CacheManagerServiceProvider.INFINISPAN_CONFIGURATION_RESOURCENAME,
				"testing-hibernatesearch-infinispan.xml"
		);
		cfg.addAnnotatedClass( SimpleEmail.class );
	}

	@Override
	protected void tearDown() throws Exception {
		nodea.close();
		nodeb.close();
	}
}
