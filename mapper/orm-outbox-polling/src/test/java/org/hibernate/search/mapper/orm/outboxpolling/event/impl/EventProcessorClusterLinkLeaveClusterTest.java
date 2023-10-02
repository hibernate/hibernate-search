/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OutboxPollingEventProcessorClusterLink#leaveCluster(AgentClusterLinkContextProvider)}.
 */
class EventProcessorClusterLinkLeaveClusterTest extends AbstractEventProcessorClusterLinkTest {
	final OutboxPollingEventProcessorClusterLink setupLink() {
		return new OutboxPollingEventProcessorClusterLink(
				SELF_REF.name, failureHandlerMock, clockMock, shardAssignmentProviderStub,
				POLLING_INTERVAL, PULSE_INTERVAL, PULSE_EXPIRATION,
				null
		);
	}

	@Test
	void didNotJoin() {
		OutboxPollingEventProcessorClusterLink link = setupLink();
		link.leaveCluster( contextMock );
	}

	@Test
	void joined_found() {
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
	void joined_notFound() {
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
