/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
abstract class AbstractEventProcessorClusterLinkTest {

	static final Instant NOW = Instant.parse( "2021-10-21T14:30:00.000Z" );
	static final Instant EARLIER = NOW.minus( 1, ChronoUnit.NANOS );
	static final Instant LATER = NOW.plus( 1, ChronoUnit.NANOS );

	static final Duration POLLING_INTERVAL = Duration.ofMillis(
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL );
	static final Duration PULSE_INTERVAL = Duration.ofMillis(
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL );
	static final Duration PULSE_EXPIRATION = Duration.ofMillis(
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION );

	static final long SELF_ID_ORDINAL = 42;

	static final UUID SELF_ID = toUUID( SELF_ID_ORDINAL );
	static final AgentReference SELF_REF = AgentReference.of( SELF_ID, "Self Agent Name" );

	@Mock
	public FailureHandler failureHandlerMock;

	@Mock
	public Clock clockMock;

	@Mock
	public OutboxEventFinder eventFinderMock;

	@Mock
	public AgentRepository repositoryMock;

	@Mock(stubOnly = true, strictness = Mock.Strictness.LENIENT)
	public AgentClusterLinkContext contextMock;

	protected final ShardAssignment.Provider shardAssignmentProviderStub =
			new ShardAssignment.Provider( new OutboxEventFinderProvider() {
				@Override
				public OutboxEventFinder create(Optional<OutboxEventPredicate> predicate) {
					return eventFinderMock;
				}

				@Override
				public void appendTo(ToStringTreeAppender appender) {
					appender.attribute( "stub", true );
				}
			} );

	private final List<Object> allMocks = new ArrayList<>();

	protected AgentRepositoryMockingHelper repositoryMockHelper;

	@BeforeEach
	final void initMocks() {
		repositoryMockHelper = new AgentRepositoryMockingHelper( repositoryMock );
		Collections.addAll( allMocks, failureHandlerMock, clockMock, eventFinderMock, repositoryMock );

		when( contextMock.agentRepository() ).thenReturn( repositoryMock );
		// We're not interested in transaction control here
		doNothing().when( contextMock ).commitAndBeginNewTransaction();
	}

	@AfterEach
	void verifyNoMoreInvocationsOnAllMocks() {
		verifyNoMoreInteractions( allMocks.toArray() );
	}

	protected void defineSelfNotCreatedYet(OutboxPollingEventProcessorClusterLink link) {
		link.getAgentPersisterForTests().setSelfReferenceForTests( null );
		repositoryMockHelper.defineSelfCreatedByPulse( SELF_ID );
	}

	protected void defineSelfCreatedAndStillPresent(OutboxPollingEventProcessorClusterLink link,
			AgentState state, ShardAssignmentDescriptor shardAssignment) {
		link.getAgentPersisterForTests().setSelfReferenceForTests( SELF_REF );
		AgentType type;
		if ( link.shardAssignmentIsStatic ) {
			type = AgentType.EVENT_PROCESSING_STATIC_SHARDING;
			if ( shardAssignment == null ) {
				shardAssignment = link.lastShardAssignment.descriptor;
			}
		}
		else {
			type = AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING;
		}
		Agent self = new Agent( type, SELF_REF.name, NOW, state, shardAssignment );
		self.setId( SELF_ID );
		repositoryMockHelper.defineSelfPreExisting( self );
	}

	protected final EventProcessorClusterLinkPulseExpectations.InstructionsStep expect(
			ShardAssignmentDescriptor selfStaticShardAssignment,
			OutboxPollingEventProcessorClusterLink link) {
		return EventProcessorClusterLinkPulseExpectations.expect( repositoryMockHelper, eventFinderMock,
				selfStaticShardAssignment, link );
	}

	public static UUID toUUID(long id) {
		return UUID.fromString( String.format( Locale.ROOT, "97b5e073-0e1f-43e9-bc84-%1$12s", id ).replace( ' ', '0' ) );
	}

}
