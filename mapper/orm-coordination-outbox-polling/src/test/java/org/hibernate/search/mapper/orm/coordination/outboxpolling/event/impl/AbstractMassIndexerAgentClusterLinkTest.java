/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

abstract class AbstractMassIndexerAgentClusterLinkTest {

	static final Instant NOW = Instant.parse( "2021-11-21T14:30:00.000Z" );
	static final Instant EARLIER = NOW.minus( 1, ChronoUnit.NANOS );
	static final Instant LATER = NOW.plus( 1, ChronoUnit.NANOS );

	static final Duration POLLING_INTERVAL = Duration.ofMillis(
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_POLLING_INTERVAL );
	static final Duration PULSE_INTERVAL = Duration.ofMillis(
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_PULSE_INTERVAL );
	static final Duration PULSE_EXPIRATION = Duration.ofMillis(
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_PULSE_EXPIRATION );

	static final long SELF_ID = 42L;
	static final AgentReference SELF_REF = AgentReference.of( SELF_ID, "Self Agent Name" );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	public FailureHandler failureHandlerMock;

	@Mock
	public Clock clockMock;

	@Mock
	public AgentRepository repositoryMock;

	private final List<Object> allMocks = new ArrayList<>();

	protected AgentRepositoryMockingHelper repositoryMockHelper;

	@Before
	public final void initMocks() {
		repositoryMockHelper = new AgentRepositoryMockingHelper( repositoryMock );
		Collections.addAll( allMocks, failureHandlerMock, clockMock, repositoryMock );
	}

	@After
	public void verifyNoMoreInvocationsOnAllMocks() {
		verifyNoMoreInteractions( allMocks.toArray() );
	}

	protected void defineSelfNotCreatedYet(OutboxPollingMassIndexerAgentClusterLink link) {
		link.agentPersister.setSelfReferenceForTests( null );
		repositoryMockHelper.defineSelfCreatedByPulse( SELF_ID );
	}

	protected void defineSelfCreatedAndStillPresent(OutboxPollingMassIndexerAgentClusterLink link,
			AgentState state) {
		link.agentPersister.setSelfReferenceForTests( SELF_REF );
		Agent self = new Agent( AgentType.MASS_INDEXING, SELF_REF.name, NOW, state, null );
		self.setId( SELF_ID );
		repositoryMockHelper.defineSelfPreExisting( self );
	}

	protected final MassIndexerAgentClusterLinkPulseExpectations.InstructionsStep expect(OutboxPollingMassIndexerAgentClusterLink link) {
		return MassIndexerAgentClusterLinkPulseExpectations.expect( repositoryMockHelper, link );
	}

}