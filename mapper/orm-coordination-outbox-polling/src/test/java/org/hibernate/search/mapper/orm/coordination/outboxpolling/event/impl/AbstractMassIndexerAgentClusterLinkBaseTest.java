/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;

import org.junit.Before;
import org.junit.Test;

/**
 * This abstract class implements one test method for each "external" situation that
 * {@link OutboxPollingMassIndexerAgentClusterLink#pulse(AgentRepository)} can encounter,
 * i.e. for each configuration of other agents as registered in the database
 * (number of other agent, state of other agents, ...).
 * <p>
 * We then have one subclass for each "internal" situation that
 * {@link OutboxPollingMassIndexerAgentClusterLink#pulse(AgentRepository)} can encounter,
 * i.e. for each state that the "self" agent can be in.
 * Each subclass defines the expectations for each test method.
 * <p>
 * By testing all these combinations, we manage to test many (all?) possible situations
 * that {@link OutboxPollingMassIndexerAgentClusterLink#pulse(AgentRepository)} can encounter.
 */
abstract class AbstractMassIndexerAgentClusterLinkBaseTest extends AbstractMassIndexerAgentClusterLinkTest {

	private static final long MASS_INDEXING_ID = 4353L;

	OutboxPollingMassIndexerAgentClusterLink link;

	@Before
	public final void initLink() {
		link = new OutboxPollingMassIndexerAgentClusterLink(
				SELF_REF.name, failureHandlerMock, clockMock,
				POLLING_INTERVAL, PULSE_INTERVAL, PULSE_EXPIRATION
		);

		defineSelf();
	}

	protected abstract void defineSelf();

	protected void defineSelfNotCreatedYet() {
		defineSelfNotCreatedYet( link );
	}

	protected void defineSelfCreatedAndStillPresent(AgentState state) {
		defineSelfCreatedAndStillPresent( link, state );
	}

	protected final MassIndexerAgentClusterLinkPulseExpectations.InstructionsStep expect() {
		return expect( link );
	}

	protected final MassIndexerAgentClusterLinkPulseExpectations expectInitialStateAndPulseAfterDelay(Duration delay) {
		return expect().pulseAgain( NOW.plus( delay ) )
				.agent( SELF_ID, repositoryMockHelper.selfInitialState() != null
						// If self created before this pulse:
						? repositoryMockHelper.selfInitialState()
						// If self created by this pulse:
						: AgentState.SUSPENDED )
				.expiration( repositoryMockHelper.selfInitialExpiration() != null
						// If self created before this pulse:
						? repositoryMockHelper.selfInitialExpiration()
						// If self created by this pulse:
						: NOW.plus( PULSE_EXPIRATION ) )
				.build();
	}

	protected final MassIndexerAgentClusterLinkPulseExpectations expectWaiting() {
		return expect().pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( SELF_ID, AgentState.WAITING )
				.build();
	}

	protected final MassIndexerAgentClusterLinkPulseExpectations expectRunning() {
		return expect().considerEventProcessingSuspendedThenPulse()
				.agent( SELF_ID, AgentState.RUNNING )
				.build();
	}

	protected abstract long other1Id();
	protected abstract long other2Id();
	protected abstract long other3Id();

	protected abstract AgentType otherType();

	protected final boolean isOtherStatic() {
		return AgentType.EVENT_PROCESSING_STATIC_SHARDING.equals( otherType() );
	}

	protected final ShardAssignmentDescriptor otherShardAssignmentIn4NodeCluster(int otherNumber) {
		switch ( otherNumber ) {
			case 1:
				return new ShardAssignmentDescriptor( 4, 0 );
			case 2:
				return new ShardAssignmentDescriptor( 4, 2 );
			case 3:
				return new ShardAssignmentDescriptor( 4, 3 );
			default:
				throw new IllegalArgumentException( "Other with number " + otherNumber + " is not in the 4 node cluster." );
		}
	}

	protected abstract MassIndexerAgentClusterLinkPulseExpectations onNoOtherAgents();

	protected abstract MassIndexerAgentClusterLinkPulseExpectations onClusterWith3NodesAll3NodesSuspended();

	@Before
	public void initPulseMocks() {
		when( repositoryMock.findAllOrderById() ).thenAnswer( ignored -> repositoryMockHelper.allAgentsInIdOrder() );
		when( clockMock.instant() ).thenReturn( NOW );
	}

	@Test
	public void noOtherAgent() {
		repositoryMockHelper.defineOtherAgents();

		onNoOtherAgents().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_someExpired() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), EARLIER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), EARLIER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		// Do not update the agent, in order to avoid locks on Oracle in particular (maybe others);
		// see the comment in AbstractAgentClusterLink#pulse.
		// We will assess the situation in the next pulse.
		expectInitialStateAndPulseAfterDelay( POLLING_INTERVAL ).verify( link.pulse( contextMock ) );

		verify( repositoryMock ).delete( repositoryMockHelper.agentsInIdOrder( other2Id(), other3Id() ) );
	}

	@Test
	public void clusterWith3Nodes_someSuspended_someWaiting() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), otherType(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_someSuspended_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_someWaiting_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_someSuspended_someWaiting_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allSuspended() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 3 ) : null );

		onClusterWith3NodesAll3NodesSuspended().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allWaiting() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_someExpired_massIndexingAgent_running() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), EARLIER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), EARLIER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.RUNNING );

		// Do not update the agent, in order to avoid locks on Oracle in particular (maybe others);
		// see the comment in AbstractAgentClusterLink#pulse.
		// We will assess the situation in the next pulse.
		expectInitialStateAndPulseAfterDelay( POLLING_INTERVAL ).verify( link.pulse( contextMock ) );

		// Cleaning up expired agents takes precedence over suspending because a mass indexing agent exists.
		verify( repositoryMock ).delete( repositoryMockHelper.agentsInIdOrder( other2Id(), other3Id() ) );
	}

	@Test
	public void clusterWith3Nodes_someSuspended_someRunning_massIndexingAgent_running() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.RUNNING );

		// The presence of a mass indexing agent is more important than rebalancing:
		// we expect the agent to suspend itself.
		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allSuspended_massIndexingAgent_running() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 3 ) : null )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.RUNNING );

		// We should ignore concurrent mass indexer agents:
		// the user is responsible for checking they don't reindex the same entity concurrently.
		onClusterWith3NodesAll3NodesSuspended().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allSuspended_massIndexingAgent_suspended() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 3 ) : null )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.SUSPENDED );

		// Suspended mass indexing agents should not exist,
		// but just for the sake of fully defining the behavior,
		// we'll say we ignore them.
		onClusterWith3NodesAll3NodesSuspended().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allSuspended_massIndexingAgent_waiting() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 3 ) : null )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.WAITING );

		// Rebalancing mass indexing agents should not exist,
		// but just for the sake of fully defining the behavior,
		// we'll say we ignore them.
		onClusterWith3NodesAll3NodesSuspended().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allSuspended_massIndexingAgent_expired() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), otherType(), LATER, AgentState.SUSPENDED,
						isOtherStatic() ? otherShardAssignmentIn4NodeCluster( 3 ) : null )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, EARLIER, AgentState.RUNNING );

		// Do not update the agent, in order to avoid locks on Oracle in particular (maybe others);
		// see the comment in AbstractAgentClusterLink#pulse.
		// We will assess the situation in the next pulse.
		expectInitialStateAndPulseAfterDelay( POLLING_INTERVAL ).verify( link.pulse( contextMock ) );

		verify( repositoryMock ).delete( repositoryMockHelper.agentsInIdOrder( MASS_INDEXING_ID ) );
	}

	@Test
	public void clusterWith3Nodes_allRunning_massIndexingAgent_running() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.RUNNING );

		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allRunning_massIndexingAgent_suspended() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.SUSPENDED );

		// Suspended mass indexing agents should not exist,
		// but just for the sake of fully defining the behavior,
		// we'll say we ignore them.
		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allRunning_massIndexingAgent_waiting() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.WAITING );

		// Rebalancing mass indexing agents should not exist,
		// but just for the sake of fully defining the behavior,
		// we'll say we ignore them.
		expectWaiting().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith3Nodes_allRunning_massIndexingAgent_expired() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), otherType(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, EARLIER, AgentState.RUNNING );

		// Do not update the agent, in order to avoid locks on Oracle in particular (maybe others);
		// see the comment in AbstractAgentClusterLink#pulse.
		// We will assess the situation in the next pulse.
		expectInitialStateAndPulseAfterDelay( POLLING_INTERVAL ).verify( link.pulse( contextMock ) );

		verify( repositoryMock ).delete( repositoryMockHelper.agentsInIdOrder( MASS_INDEXING_ID ) );
	}

}