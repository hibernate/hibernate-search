/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;

import org.junit.Before;
import org.junit.Test;

/**
 * This abstract class implements one test method for each "external" situation that
 * {@link OutboxPollingEventProcessorClusterLink#pulse(AgentClusterLinkContext)} can encounter,
 * i.e. for each configuration of other agents as registered in the database
 * (number of other agent, state of other agents, ...).
 * <p>
 * We then have one subclass for each "internal" situation that
 * {@link OutboxPollingEventProcessorClusterLink#pulse(AgentClusterLinkContext)} can encounter,
 * i.e. for each state that the "self" agent can be in.
 * Each subclass defines the expectations for each test method.
 * <p>
 * By testing all these combinations, we manage to test many (all?) possible situations
 * that {@link OutboxPollingEventProcessorClusterLink#pulse(AgentClusterLinkContext)} can encounter.
 */
abstract class AbstractEventProcessorClusterLinkBaseTest extends AbstractEventProcessorClusterLinkTest {

	private static final UUID MASS_INDEXING_ID = UUID.fromString( "99999999-0e1f-43e9-bc84-000000004353" );

	OutboxPollingEventProcessorClusterLink link;

	@Before
	public final void initLink() {
		link = new OutboxPollingEventProcessorClusterLink(
				SELF_REF.name, failureHandlerMock, clockMock, shardAssignmentProviderStub,
				POLLING_INTERVAL, PULSE_INTERVAL, PULSE_EXPIRATION,
				selfStaticShardAssignment()
		);

		defineSelf();
	}

	protected abstract void defineSelf();

	protected void defineSelfNotCreatedYet() {
		defineSelfNotCreatedYet( link );
	}

	protected void defineSelfCreatedAndStillPresent(AgentState state,
			ShardAssignmentDescriptor shardAssignment) {
		defineSelfCreatedAndStillPresent( link, state, shardAssignment );
	}

	protected final EventProcessorClusterLinkPulseExpectations.InstructionsStep expect() {
		return expect( selfStaticShardAssignment(), link );
	}

	protected final EventProcessorClusterLinkPulseExpectations expectSuspendedAndPulseAfterDelay(Duration delay) {
		return expect().pulseAgain( NOW.plus( delay ) )
				.agent( SELF_ID, AgentState.SUSPENDED )
				.shardAssignment( selfStaticShardAssignment() )
				.build();
	}

	protected final EventProcessorClusterLinkPulseExpectations expectInitialStateAndPulseAfterDelay(Duration delay) {
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
				.shardAssignment( selfStaticShardAssignment() != null
						? selfStaticShardAssignment()
						: repositoryMockHelper.selfInitialShardAssignment() )
				.build();
	}

	protected final EventProcessorClusterLinkPulseExpectations expectWaiting(ShardAssignmentDescriptor shardAssignment) {
		return expect().pulseAgain( NOW.plus( POLLING_INTERVAL ) )
				.agent( SELF_ID, AgentState.WAITING )
				.shardAssignment( shardAssignment )
				.build();
	}

	protected final EventProcessorClusterLinkPulseExpectations expectRunning(ShardAssignmentDescriptor shardAssignment) {
		return expect().processThenPulse( shardAssignment )
				.agent( SELF_ID, AgentState.RUNNING )
				.shardAssignment( shardAssignment )
				.build();
	}

	protected abstract UUID other1Id();

	protected abstract UUID other2Id();

	protected abstract UUID other3Id();

	protected abstract AgentType other1Type();

	protected abstract AgentType selfType();

	protected abstract AgentType other2Type();

	protected abstract AgentType other3Type();

	protected final boolean isOther1Static() {
		return AgentType.EVENT_PROCESSING_STATIC_SHARDING.equals( other1Type() );
	}

	protected final boolean isOther2Static() {
		return AgentType.EVENT_PROCESSING_STATIC_SHARDING.equals( other2Type() );
	}

	protected final boolean isOther3Static() {
		return AgentType.EVENT_PROCESSING_STATIC_SHARDING.equals( other3Type() );
	}

	protected final ShardAssignmentDescriptor selfShardAssignmentIn1NodeCluster() {
		return new ShardAssignmentDescriptor( 1, 0 );
	}

	protected final ShardAssignmentDescriptor selfShardAssignmentIn3NodeCluster() {
		return new ShardAssignmentDescriptor( 3, 1 );
	}

	protected final ShardAssignmentDescriptor selfShardAssignmentIn4NodeCluster() {
		return new ShardAssignmentDescriptor( 4, 1 );
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

	protected final ShardAssignmentDescriptor selfStaticShardAssignment() {
		return AgentType.EVENT_PROCESSING_STATIC_SHARDING.equals( selfType() )
				? selfShardAssignmentIn4NodeCluster()
				: null;
	}

	protected final ShardAssignmentDescriptor shardAssignmentIn5NodeCluster() {
		return new ShardAssignmentDescriptor( 5, 1 );
	}

	protected abstract EventProcessorClusterLinkPulseExpectations onNoOtherAgents();

	protected abstract EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady();

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
	public void clusterWith4Nodes_someExpired() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), EARLIER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), EARLIER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		// Do not update the agent, in order to avoid locks on Oracle in particular (maybe others);
		// see the comment in AbstractAgentClusterLink#pulse.
		// We will assess the situation in the next pulse.
		expectInitialStateAndPulseAfterDelay( POLLING_INTERVAL ).verify( link.pulse( contextMock ) );

		verify( repositoryMock ).delete( repositoryMockHelper.agentsInIdOrder( other2Id(), other3Id() ) );
	}

	@Test
	public void clusterWith4Nodes_someSuspended_someWaiting() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.SUSPENDED,
						isOther2Static() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), other3Type(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_someSuspended_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.SUSPENDED,
						isOther2Static() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), other3Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_someWaiting_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		onClusterWith4NodesAllOther3NodesReady().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_someSuspended_someWaiting_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.SUSPENDED,
						isOther1Static() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), other2Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectWaiting( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_allSuspended() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.SUSPENDED,
						isOther1Static() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), other2Type(), LATER, AgentState.SUSPENDED,
						isOther2Static() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), other3Type(), LATER, AgentState.SUSPENDED,
						isOther3Static() ? otherShardAssignmentIn4NodeCluster( 3 ) : null );

		expectWaiting( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_allWaiting() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, AgentState.WAITING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		onClusterWith4NodesAllOther3NodesReady().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_allRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		onClusterWith4NodesAllOther3NodesReady().verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_someExpired_massIndexingAgent_running() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), EARLIER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), EARLIER, AgentState.RUNNING,
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
	public void clusterWith4Nodes_someSuspended_someRunning_massIndexingAgent_running() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.SUSPENDED,
						isOther2Static() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), other3Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.RUNNING );

		// The presence of a mass indexing agent is more important than rebalancing:
		// we expect the agent to suspend itself.
		expectSuspendedAndPulseAfterDelay( PULSE_INTERVAL ).verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_allRunning_massIndexingAgent_running() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.RUNNING );

		expectSuspendedAndPulseAfterDelay( PULSE_INTERVAL ).verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_allRunning_massIndexingAgent_suspended() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.SUSPENDED );

		// Suspended mass indexing agents should not exist,
		// but just for the sake of fully defining the behavior,
		// we'll say they prevent automatic indexing too.
		expectSuspendedAndPulseAfterDelay( PULSE_INTERVAL ).verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_allRunning_massIndexingAgent_waiting() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, LATER, AgentState.WAITING );

		// Rebalancing mass indexing agents should not exist,
		// but just for the sake of fully defining the behavior,
		// we'll say they prevent automatic indexing too.
		expectSuspendedAndPulseAfterDelay( PULSE_INTERVAL ).verify( link.pulse( contextMock ) );
	}

	@Test
	public void clusterWith4Nodes_allRunning_massIndexingAgent_expired() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, AgentState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) )
				.other( MASS_INDEXING_ID, AgentType.MASS_INDEXING, EARLIER, AgentState.RUNNING );

		// Do not update the agent, in order to avoid locks on Oracle in particular (maybe others);
		// see the comment in AbstractAgentClusterLink#pulse.
		// We will assess the situation in the next pulse.
		expectInitialStateAndPulseAfterDelay( POLLING_INTERVAL ).verify( link.pulse( contextMock ) );

		verify( repositoryMock ).delete( repositoryMockHelper.agentsInIdOrder( MASS_INDEXING_ID ) );
	}

}
