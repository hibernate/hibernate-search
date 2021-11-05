/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl.EventProcessingState;
import org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl.ShardAssignmentDescriptor;

import org.junit.Before;
import org.junit.Test;

/**
 * This abstract class implements one test method for each "external" situation that
 * {@link OutboxEventBackgroundProcessorClusterLink#pulse(AgentRepository)} can encounter,
 * i.e. for each configuration of other agents as registered in the database
 * (number of other agent, state of other agents, ...).
 * <p>
 * We then have one subclass for each "internal" situation that
 * {@link OutboxEventBackgroundProcessorClusterLink#pulse(AgentRepository)} can encounter,
 * i.e. for each state that the "self" agent can be in.
 * Each subclass defines the expectations for each test method.
 * <p>
 * By testing all these combinations, we manage to test many (all?) possible situations
 * that {@link OutboxEventBackgroundProcessorClusterLink#pulse(AgentRepository)} can encounter.
 */
abstract class AbstractClusterLinkBaseTest extends AbstractClusterLinkTest {

	OutboxEventBackgroundProcessorClusterLink link;

	@Before
	public final void initLink() {
		link = new OutboxEventBackgroundProcessorClusterLink(
				SELF_REF.name, failureHandlerMock, clockMock, eventFinderProviderStub,
				PULSE_INTERVAL, PULSE_EXPIRATION,
				selfStaticShardAssignment()
		);

		defineSelf();
	}

	protected abstract void defineSelf();

	protected void defineSelfNotCreatedYet() {
		defineSelfNotCreatedYet( link );
	}

	protected void defineSelfCreatedAndStillPresent(EventProcessingState state,
			ShardAssignmentDescriptor shardAssignment) {
		defineSelfCreatedAndStillPresent( link, state, shardAssignment );
	}

	protected final ClusterLinkPulseExpectations.InstructionsStep expect() {
		return expect( selfStaticShardAssignment(), link );
	}

	protected final ClusterLinkPulseExpectations expectSuspendedAndPulseASAP() {
		return expect().pulseAgain( NOW )
				.agent( SELF_ID, EventProcessingState.SUSPENDED )
				.shardAssignment( selfStaticShardAssignment() )
				.build();
	}

	protected final ClusterLinkPulseExpectations expectRebalancing(ShardAssignmentDescriptor shardAssignment) {
		return expect().pulseAgain( NOW )
				.agent( SELF_ID, EventProcessingState.REBALANCING )
				.shardAssignment( shardAssignment )
				.build();
	}

	protected final ClusterLinkPulseExpectations expectRunning(ShardAssignmentDescriptor shardAssignment) {
		return expect().processThenPulse( shardAssignment )
				.agent( SELF_ID, EventProcessingState.RUNNING )
				.shardAssignment( shardAssignment )
				.build();
	}

	protected abstract long other1Id();
	protected abstract long other2Id();
	protected abstract long other3Id();

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
				? selfShardAssignmentIn4NodeCluster() : null;
	}

	protected final ShardAssignmentDescriptor shardAssignmentIn5NodeCluster() {
		return new ShardAssignmentDescriptor( 5, 1 );
	}

	protected abstract ClusterLinkPulseExpectations onNoOtherAgents();

	protected abstract ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady();

	@Before
	public void initPulseMocks() {
		when( repositoryMock.findAllOrderById() ).thenAnswer( ignored -> repositoryMockHelper.allAgentsInIdOrder() );
		when( clockMock.instant() ).thenReturn( NOW );
	}

	@Test
	public void noOtherAgent() {
		repositoryMockHelper.defineOtherAgents();

		onNoOtherAgents().verify( link.pulse( repositoryMock ) );
	}

	@Test
	public void clusterWith4Nodes_someExpired() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), EARLIER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), EARLIER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		// Suspend the agent: we will assess the situation in the next pulse
		expectSuspendedAndPulseASAP().verify( link.pulse( repositoryMock ) );

		verify( repositoryMock ).delete( repositoryMockHelper.agentsInIdOrder( other2Id(), other3Id() ) );
	}

	@Test
	public void clusterWith4Nodes_someSuspended_someRebalancing() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, EventProcessingState.REBALANCING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, EventProcessingState.SUSPENDED,
						isOther2Static() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), other3Type(), LATER, EventProcessingState.REBALANCING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectRebalancing( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( repositoryMock ) );
	}

	@Test
	public void clusterWith4Nodes_someSuspended_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, EventProcessingState.SUSPENDED,
						isOther2Static() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), other3Type(), LATER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectRebalancing( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( repositoryMock ) );
	}

	@Test
	public void clusterWith4Nodes_someRebalancing_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, EventProcessingState.REBALANCING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, EventProcessingState.REBALANCING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		onClusterWith4NodesAllOther3NodesReady().verify( link.pulse( repositoryMock ) );
	}

	@Test
	public void clusterWith4Nodes_someSuspended_someRebalancing_someRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, EventProcessingState.SUSPENDED,
						isOther1Static() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), other2Type(), LATER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, EventProcessingState.REBALANCING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		expectRebalancing( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( repositoryMock ) );
	}

	@Test
	public void clusterWith4Nodes_allSuspended() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, EventProcessingState.SUSPENDED,
						isOther1Static() ? otherShardAssignmentIn4NodeCluster( 1 ) : null )
				.other( other2Id(), other2Type(), LATER, EventProcessingState.SUSPENDED,
						isOther2Static() ? otherShardAssignmentIn4NodeCluster( 2 ) : null )
				.other( other3Id(), other3Type(), LATER, EventProcessingState.SUSPENDED,
						isOther3Static() ? otherShardAssignmentIn4NodeCluster( 3 ) : null );

		expectRebalancing( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( repositoryMock ) );
	}

	@Test
	public void clusterWith4Nodes_allRebalancing() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, EventProcessingState.REBALANCING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, EventProcessingState.REBALANCING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, EventProcessingState.REBALANCING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		onClusterWith4NodesAllOther3NodesReady().verify( link.pulse( repositoryMock ) );
	}

	@Test
	public void clusterWith4Nodes_allRunning() {
		repositoryMockHelper.defineOtherAgents()
				.other( other1Id(), other1Type(), LATER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 1 ) )
				.other( other2Id(), other2Type(), LATER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 2 ) )
				.other( other3Id(), other3Type(), LATER, EventProcessingState.RUNNING,
						otherShardAssignmentIn4NodeCluster( 3 ) );

		onClusterWith4NodesAllOther3NodesReady().verify( link.pulse( repositoryMock ) );
	}

}