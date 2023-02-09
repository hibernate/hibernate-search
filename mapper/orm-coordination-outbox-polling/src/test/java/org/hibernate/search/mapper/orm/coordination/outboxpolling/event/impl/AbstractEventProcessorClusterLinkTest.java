/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

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
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

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

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

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
				public void appendTo(ToStringTreeBuilder builder) {
					builder.attribute( "stub", true );
				}
			} );

	private final List<Object> allMocks = new ArrayList<>();

	protected AgentRepositoryMockingHelper repositoryMockHelper;

	@Before
	public final void initMocks() {
		repositoryMockHelper = new AgentRepositoryMockingHelper( repositoryMock );
		Collections.addAll( allMocks, failureHandlerMock, clockMock, eventFinderMock, repositoryMock );

		when( contextMock.agentRepository() ).thenReturn( repositoryMock );
		// We're not interested in transaction control here
		doNothing().when( contextMock ).commitAndBeginNewTransaction();
	}

	@After
	public void verifyNoMoreInvocationsOnAllMocks() {
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

	protected final EventProcessorClusterLinkPulseExpectations.InstructionsStep expect(ShardAssignmentDescriptor selfStaticShardAssignment,
			OutboxPollingEventProcessorClusterLink link) {
		return EventProcessorClusterLinkPulseExpectations.expect( repositoryMockHelper, eventFinderMock, selfStaticShardAssignment, link );
	}

	public static UUID toUUID(long id) {
		return UUID.fromString( String.format( Locale.ROOT, "97b5e073-0e1f-43e9-bc84-%1$12s", id ).replace( ' ', '0' ) );
	}

}