/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

/**
 * Tests edge cases of static sharding.
 */
class EventProcessorClusterLinkStaticShardingEdgeCasesTest
		extends AbstractEventProcessorClusterLinkTest {
	private static final UUID OTHER_0_ID = toUUID( SELF_ID_ORDINAL - 1 );
	private static final UUID OTHER_1_ID = toUUID( SELF_ID_ORDINAL + 1 );
	private static final UUID OTHER_2_ID = toUUID( SELF_ID_ORDINAL + 2 );
	private static final UUID OTHER_3_ID = toUUID( SELF_ID_ORDINAL + 3 );
	private static final UUID OTHER_4_ID = toUUID( SELF_ID_ORDINAL + 4 );

	final OutboxPollingEventProcessorClusterLink setupLink(ShardAssignmentDescriptor staticShardAssignment) {
		return new OutboxPollingEventProcessorClusterLink(
				SELF_REF.name, failureHandlerMock, clockMock, shardAssignmentProviderStub,
				POLLING_INTERVAL, PULSE_INTERVAL, PULSE_EXPIRATION,
				staticShardAssignment
		);
	}

	@BeforeEach
	void initPulseMocks() {
		when( repositoryMock.findAllOrderById() ).thenAnswer( ignored -> repositoryMockHelper.allAgentsInIdOrder() );
		when( clockMock.instant() ).thenReturn( NOW );
	}

	@Test
	void selfExpires_rejoin() {
		OutboxPollingEventProcessorClusterLink link = setupLink( null );
		defineSelfNotCreatedYet( link );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( 4, 1 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( 4, 2 ) )
				.other( OTHER_3_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( 4, 3 ) );

		expect( null, link )
				.pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( SELF_ID, AgentState.WAITING )
				.shardAssignment( new ShardAssignmentDescriptor( 4, 0 ) )
				.build()
				.verify( link.pulse( contextMock ) );

		verifyNoMoreInvocationsOnAllMocks();

		// Simulate a deletion by another agent (because of expiration, for example)
		UUID newId = toUUID( SELF_ID_ORDINAL + 100 );
		repositoryMockHelper.defineSelfCreatedByPulse( newId );

		expect( null, link )
				.pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( newId, AgentState.WAITING )
				.shardAssignment( new ShardAssignmentDescriptor( 4, 3 ) )
				.build()
				.verify( link.pulse( contextMock ) );

		verify( repositoryMock ).create( repositoryMockHelper.self() );
	}

	@Test
	void staticSharding_conflictingAssignedShardIds() {
		int totalShardCount = 3;
		ShardAssignmentDescriptor selfStaticShardAssignment =
				new ShardAssignmentDescriptor( totalShardCount, 1 );

		OutboxPollingEventProcessorClusterLink link = setupLink( selfStaticShardAssignment );
		defineSelfNotCreatedYet( link );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.SUSPENDED,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.SUSPENDED,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) );

		expect( selfStaticShardAssignment, link )
				.pulseAgain( NOW.plus( PULSE_INTERVAL ) )
				.agent( SELF_ID, AgentState.SUSPENDED )
				.shardAssignment( selfStaticShardAssignment )
				.build()
				.verify( link.pulse( contextMock ) );

		ArgumentCaptor<FailureContext> failureCaptor = ArgumentCaptor.forClass( FailureContext.class );
		verify( failureHandlerMock ).handle( failureCaptor.capture() );
		FailureContext failure = failureCaptor.getValue();
		assertThat( failure.failingOperation() )
				.isEqualTo( "Pulse operation for agent '" + SELF_REF + "'" );
		assertThat( failure.throwable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Agent '" + SELF_REF + "': failed to infer a target cluster from the list of registered agents.",
						"The agent will try again in the next pulse.",
						"Agent '" + OTHER_2_ID + " - ",
						"is statically assigned to shard 0 (total " + totalShardCount + ")",
						"this conflicts with agent '" + OTHER_1_ID + " - ",
						"' which is also assigned to that shard.",
						"This can be a temporary situation caused by some application instances being forcibly stopped and replacements being spun up",
						"consider adjusting the configuration or switching to dynamic sharding.",
						"Registered agents:" );
	}

	@Test
	void staticSharding_conflictingTotalShardCount() {
		ShardAssignmentDescriptor selfStaticShardAssignment =
				new ShardAssignmentDescriptor( 3, 1 );

		OutboxPollingEventProcessorClusterLink link = setupLink( selfStaticShardAssignment );
		defineSelfNotCreatedYet( link );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( 3, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( 4, 2 ) );

		expect( selfStaticShardAssignment, link )
				.pulseAgain( NOW.plus( PULSE_INTERVAL ) )
				.agent( SELF_ID, AgentState.SUSPENDED )
				.shardAssignment( selfStaticShardAssignment )
				.build()
				.verify( link.pulse( contextMock ) );

		ArgumentCaptor<FailureContext> failureCaptor = ArgumentCaptor.forClass( FailureContext.class );
		verify( failureHandlerMock ).handle( failureCaptor.capture() );
		FailureContext failure = failureCaptor.getValue();
		assertThat( failure.failingOperation() )
				.isEqualTo( "Pulse operation for agent '" + SELF_REF + "'" );
		assertThat( failure.throwable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Agent '" + SELF_REF + "': failed to infer a target cluster from the list of registered agents.",
						"The agent will try again in the next pulse.",
						"Agent '" + OTHER_2_ID + " - ",
						"is statically assigned to shard 2 (total 4)",
						"this conflicts with agent '" + SELF_REF + "'",
						"which expects 3 shards.",
						"This can be a temporary situation caused by some application instances being forcibly stopped and replacements being spun up",
						"consider adjusting the configuration or switching to dynamic sharding.",
						"Registered agents:" );
	}

	@Test
	void mixedSharding_otherDynamicSuperfluous_selfWaiting_includedAgentsReady_extraAgentsSuspended() {
		int totalShardCount = 4;
		ShardAssignmentDescriptor selfStaticShardAssignment =
				new ShardAssignmentDescriptor( totalShardCount, 1 );

		OutboxPollingEventProcessorClusterLink link = setupLink( selfStaticShardAssignment );
		defineSelfCreatedAndStillPresent( link, AgentState.WAITING, selfStaticShardAssignment );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 3 ) )
				.other( OTHER_3_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 2 ) )
				.other( OTHER_4_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.SUSPENDED );

		expect( selfStaticShardAssignment, link )
				.processThenPulse( selfStaticShardAssignment )
				.agent( SELF_ID, AgentState.RUNNING )
				.shardAssignment( selfStaticShardAssignment )
				.build()
				.verify( link.pulse( contextMock ) );
	}

	@Test
	void mixedSharding_otherDynamicSuperfluous_selfSuspended_includedAgentsReady_extraAgentsSuspended() {
		int totalShardCount = 4;
		ShardAssignmentDescriptor selfStaticShardAssignment =
				new ShardAssignmentDescriptor( totalShardCount, 1 );

		OutboxPollingEventProcessorClusterLink link = setupLink( selfStaticShardAssignment );
		defineSelfCreatedAndStillPresent( link, AgentState.SUSPENDED, null );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 3 ) )
				.other( OTHER_3_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 2 ) )
				.other( OTHER_4_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.SUSPENDED );

		expect( selfStaticShardAssignment, link )
				.pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( SELF_ID, AgentState.WAITING )
				.shardAssignment( selfStaticShardAssignment )
				.build()
				.verify( link.pulse( contextMock ) );
	}

	@Test
	void mixedSharding_otherDynamicSuperfluous_selfWaiting_includedAgentSuspended_extraAgentsSuspended() {
		int totalShardCount = 4;
		ShardAssignmentDescriptor selfStaticShardAssignment =
				new ShardAssignmentDescriptor( totalShardCount, 1 );

		OutboxPollingEventProcessorClusterLink link = setupLink( selfStaticShardAssignment );
		defineSelfCreatedAndStillPresent( link, AgentState.WAITING, selfStaticShardAssignment );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.SUSPENDED,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 3 ) )
				.other( OTHER_3_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 2 ) )
				.other( OTHER_4_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.SUSPENDED );

		expect( selfStaticShardAssignment, link )
				.pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( SELF_ID, AgentState.WAITING )
				.shardAssignment( selfStaticShardAssignment )
				.build()
				.verify( link.pulse( contextMock ) );
	}

	@Test
	void mixedSharding_otherDynamicSuperfluous_selfWaiting_includedAgentInWrongCluster_extraAgentsSuspended() {
		int totalShardCount = 4;
		ShardAssignmentDescriptor selfStaticShardAssignment =
				new ShardAssignmentDescriptor( totalShardCount, 1 );

		OutboxPollingEventProcessorClusterLink link = setupLink( selfStaticShardAssignment );
		defineSelfCreatedAndStillPresent( link, AgentState.WAITING, selfStaticShardAssignment );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 3 ) )
				.other( OTHER_3_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 1 ) )
				.other( OTHER_4_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.SUSPENDED );

		expect( selfStaticShardAssignment, link )
				.pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( SELF_ID, AgentState.WAITING )
				.shardAssignment( selfStaticShardAssignment )
				.build()
				.verify( link.pulse( contextMock ) );
	}

	@Test
	void mixedSharding_otherDynamicSuperfluous_selfWaiting_includedAgentsReady_extraAgentsRunning() {
		int totalShardCount = 4;
		ShardAssignmentDescriptor selfStaticShardAssignment =
				new ShardAssignmentDescriptor( totalShardCount, 1 );

		OutboxPollingEventProcessorClusterLink link = setupLink( selfStaticShardAssignment );
		defineSelfCreatedAndStillPresent( link, AgentState.WAITING, selfStaticShardAssignment );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 3 ) )
				.other( OTHER_3_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 2 ) )
				.other( OTHER_4_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.RUNNING,
						new ShardAssignmentDescriptor( 1, 0 ) );

		expect( selfStaticShardAssignment, link )
				.pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( SELF_ID, AgentState.WAITING )
				.shardAssignment( selfStaticShardAssignment )
				.build()
				.verify( link.pulse( contextMock ) );
	}

	@Test
	void mixedSharding_otherDynamicSuperfluous_selfWaiting_includedAgentsReady_extraAgentsWaiting() {
		int totalShardCount = 4;
		ShardAssignmentDescriptor selfStaticShardAssignment =
				new ShardAssignmentDescriptor( totalShardCount, 1 );

		OutboxPollingEventProcessorClusterLink link = setupLink( selfStaticShardAssignment );
		defineSelfCreatedAndStillPresent( link, AgentState.WAITING, selfStaticShardAssignment );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 3 ) )
				.other( OTHER_3_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 2 ) )
				.other( OTHER_4_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( 1, 0 ) );

		expect( selfStaticShardAssignment, link )
				.pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( SELF_ID, AgentState.WAITING )
				.shardAssignment( selfStaticShardAssignment )
				.build()
				.verify( link.pulse( contextMock ) );
	}

	@Test
	void mixedSharding_selfDynamicSuperfluous_selfWaiting_includedAgentsReady_extraAgentsSuspended() {
		int totalShardCount = 4;

		OutboxPollingEventProcessorClusterLink link = setupLink( null );
		defineSelfCreatedAndStillPresent( link, AgentState.WAITING,
				new ShardAssignmentDescriptor( totalShardCount, 2 ) );

		repositoryMockHelper.defineOtherAgents()
				.other( OTHER_1_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 0 ) )
				.other( OTHER_2_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 3 ) )
				.other( OTHER_0_ID, AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING, LATER, AgentState.WAITING,
						new ShardAssignmentDescriptor( totalShardCount, 2 ) )
				.other( OTHER_4_ID, AgentType.EVENT_PROCESSING_STATIC_SHARDING, LATER, AgentState.SUSPENDED,
						new ShardAssignmentDescriptor( totalShardCount, 1 ) );

		expect( null, link )
				.pulseAgain( NOW.plus( PULSE_INTERVAL ) )
				.agent( SELF_ID, AgentState.SUSPENDED )
				.build()
				.verify( link.pulse( contextMock ) );
	}

}
