/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl.AgentRepository;

import org.junit.Test;

/**
 * Tests for {@link OutboxEventBackgroundProcessorClusterLink#leaveCluster(AgentRepository)}.
 */
public class ClusterLinkLeaveClusterTest extends AbstractClusterLinkTest {
	final OutboxEventBackgroundProcessorClusterLink setupLink() {
		return new OutboxEventBackgroundProcessorClusterLink(
				SELF_REF.name, failureHandlerMock, clockMock, eventFinderProviderStub,
				PULSE_INTERVAL, PULSE_EXPIRATION,
				null
		);
	}

	@Test
	public void didNotJoin() {
		OutboxEventBackgroundProcessorClusterLink link = setupLink();
		link.leaveCluster( repositoryMock );
	}

	@Test
	public void joined_found() {
		OutboxEventBackgroundProcessorClusterLink link = setupLink();
		defineSelfNotCreatedYet( link );
		repositoryMockHelper.defineOtherAgents();
		when( repositoryMock.findAllOrderById() ).thenAnswer( ignored -> repositoryMockHelper.allAgentsInIdOrder() );
		when( clockMock.instant() ).thenReturn( NOW );
		link.pulse( repositoryMock );

		when( repositoryMock.find( SELF_ID ) ).thenReturn( repositoryMockHelper.self() );
		link.leaveCluster( repositoryMock );
		verify( repositoryMock ).delete( Collections.singletonList( repositoryMockHelper.self() ) );
	}

	@Test
	public void joined_notFound() {
		OutboxEventBackgroundProcessorClusterLink link = setupLink();
		defineSelfNotCreatedYet( link );
		repositoryMockHelper.defineOtherAgents();
		when( repositoryMock.findAllOrderById() ).thenAnswer( ignored -> repositoryMockHelper.allAgentsInIdOrder() );
		when( clockMock.instant() ).thenReturn( NOW );
		link.pulse( repositoryMock );

		when( repositoryMock.find( SELF_ID ) ).thenReturn( null );
		link.leaveCluster( repositoryMock );
	}

}