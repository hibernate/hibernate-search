/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.EventProcessingState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 * Base tests of {@link OutboxPollingEventProcessorClusterLink}
 * with dynamic sharding.
 */
@RunWith(Enclosed.class)
public class ClusterLinkDynamicShardingBaseTest {

	abstract static class AbstractBaseTest extends AbstractClusterLinkBaseTest {
		// Define IDs in ascending order, because IDs matter when using dynamic sharding.

		@Override
		protected long other1Id() {
			return AbstractClusterLinkTest.SELF_ID - 1;
		}

		@Override
		protected long other2Id() {
			return AbstractClusterLinkTest.SELF_ID + 1;
		}

		@Override
		protected long other3Id() {
			return AbstractClusterLinkTest.SELF_ID + 2;
		}

		@Override
		protected AgentType other1Type() {
			return AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING;
		}

		@Override
		protected AgentType selfType() {
			return AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING;
		}

		@Override
		protected AgentType other2Type() {
			return AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING;
		}

		@Override
		protected AgentType other3Type() {
			return AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING;
		}

		@Test
		public void clusterWith4Nodes_allRebalancing_clusterExcludingSelf() {
			repositoryMockHelper.defineOtherAgents()
					.other( other1Id(), other1Type(), LATER, EventProcessingState.REBALANCING,
							new ShardAssignmentDescriptor( 3, 0 ) )
					.other( other2Id(), other2Type(), LATER, EventProcessingState.REBALANCING,
							new ShardAssignmentDescriptor( 3, 1 ) )
					.other( other3Id(), other3Type(), LATER, EventProcessingState.REBALANCING,
							new ShardAssignmentDescriptor( 3, 2 ) );

			expectRebalancing( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( repositoryMock ) );
		}

		@Test
		public void clusterWith4Nodes_allRunning_clusterExcludingSelf() {
			repositoryMockHelper.defineOtherAgents()
					.other( other1Id(), other1Type(), LATER, EventProcessingState.RUNNING,
							new ShardAssignmentDescriptor( 3, 0 ) )
					.other( other2Id(), other2Type(), LATER, EventProcessingState.RUNNING,
							new ShardAssignmentDescriptor( 3, 1 ) )
					.other( other3Id(), other3Type(), LATER, EventProcessingState.RUNNING,
							new ShardAssignmentDescriptor( 3, 2 ) );

			expectRebalancing( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( repositoryMock ) );
		}
	}

	// It's very important that an agent that wasn't registered
	// must not start running immediately on the first pulse,
	// but should go through the rebalancing state first.
	// See comments in OutboxEventBackgroundProcessorClusterLink.
	public static class NotRegisteredTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfNotCreatedYet();
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRebalancing( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class SuspendedTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.SUSPENDED, null );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRebalancing( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class RebalancingIn1NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.REBALANCING, selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			// We were already rebalancing and had the correct cluster => we can run now!
			return expectRunning( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class RunningIn1NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.RUNNING, selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRunning( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class RebalancingIn3NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.REBALANCING, selfShardAssignmentIn3NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRebalancing( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class RunningIn3NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.RUNNING, selfShardAssignmentIn3NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRebalancing( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class RebalancingIn4NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.REBALANCING, selfShardAssignmentIn4NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRebalancing( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			// We were already rebalancing and had the correct cluster => we can run now!
			return expectRunning( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class RunningIn4NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.RUNNING, selfShardAssignmentIn4NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRebalancing( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			// We were already running and had the correct cluster => we can run now!
			return expectRunning( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class RebalancingIn5NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.REBALANCING, shardAssignmentIn5NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRebalancing( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class RunningIn5NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.RUNNING, shardAssignmentIn5NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRebalancing( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

}
