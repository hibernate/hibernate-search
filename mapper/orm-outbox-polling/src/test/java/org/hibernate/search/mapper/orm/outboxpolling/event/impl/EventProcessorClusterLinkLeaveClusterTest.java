/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;

/**
 * Tests for {@link OutboxPollingEventProcessorClusterLink#leaveCluster(AgentClusterLinkContextProvider)}.
 */
public class EventProcessorClusterLinkLeaveClusterTest extends AbstractEventProcessorClusterLinkTest {
	final OutboxPollingEventProcessorClusterLink setupLink() {
		return new OutboxPollingEventProcessorClusterLink(
				SELF_REF.name, failureHandlerMock, clockMock, shardAssignmentProviderStub,
				POLLING_INTERVAL, PULSE_INTERVAL, PULSE_EXPIRATION,
				null
		);
	}

	@Test
	public void didNotJoin() {
		OutboxPollingEventProcessorClusterLink link = setupLink();
		link.leaveCluster( contextMock );
	}

	@Test
	public void joined_found() {
		OutboxPollingEventProcessorClusterLink link = setupLink();
		defineSelfNotCreatedYet( link );
		repositoryMockHelper.defineOtherAgents();
		when( repositoryMock.findAllOrderById() ).thenAnswer( ignored -> repositoryMockHelper.allAgentsInIdOrder() );
		when( clockMock.instant() ).thenReturn( NOW );
		link.pulse( contextMock );

		when( repositoryMock.find( SELF_ID ) ).thenReturn( repositoryMockHelper.self() );
		link.leaveCluster( contextMock );
		verify( repositoryMock ).delete( Collections.singletonList( repositoryMockHelper.self() ) );
	}

	@Test
	public void joined_notFound() {
		OutboxPollingEventProcessorClusterLink link = setupLink();
		defineSelfNotCreatedYet( link );
		repositoryMockHelper.defineOtherAgents();
		when( repositoryMock.findAllOrderById() ).thenAnswer( ignored -> repositoryMockHelper.allAgentsInIdOrder() );
		when( clockMock.instant() ).thenReturn( NOW );
		link.pulse( contextMock );

		when( repositoryMock.find( SELF_ID ) ).thenReturn( null );
		link.leaveCluster( contextMock );
	}

}
