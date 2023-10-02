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
 * Tests for {@link OutboxPollingMassIndexerAgentClusterLink#leaveCluster(AgentClusterLinkContextProvider)}.
 */
class MassIndexerAgentClusterLinkLeaveClusterTest extends AbstractMassIndexerAgentClusterLinkTest {
	final OutboxPollingMassIndexerAgentClusterLink setupLink() {
		return new OutboxPollingMassIndexerAgentClusterLink(
				SELF_REF.name, failureHandlerMock, clockMock,
				POLLING_INTERVAL, PULSE_INTERVAL, PULSE_EXPIRATION
		);
	}

	@Test
	void didNotJoin() {
		OutboxPollingMassIndexerAgentClusterLink link = setupLink();
		link.leaveCluster( contextMock );
	}

	@Test
	void joined_found() {
		OutboxPollingMassIndexerAgentClusterLink link = setupLink();
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
		OutboxPollingMassIndexerAgentClusterLink link = setupLink();
		defineSelfNotCreatedYet( link );
		repositoryMockHelper.defineOtherAgents();
		when( repositoryMock.findAllOrderById() ).thenAnswer( ignored -> repositoryMockHelper.allAgentsInIdOrder() );
		when( clockMock.instant() ).thenReturn( NOW );
		link.pulse( contextMock );

		when( repositoryMock.find( SELF_ID ) ).thenReturn( null );
		link.leaveCluster( contextMock );
	}

}
