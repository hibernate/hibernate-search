/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.EventProcessingState;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 * Base tests of {@link OutboxPollingEventProcessorClusterLink}
 * with static sharding with a total shard count of 4.
 */
@RunWith(Enclosed.class)
public class ClusterLinkStaticSharding4ShardBaseTest {

	abstract static class AbstractBaseTest extends AbstractClusterLinkBaseTest {

		// Define IDs of statically agents in the opposite order of the shard order,
		// to demonstrate that the ID order doesn't matter when using static sharding:

		@Override
		protected long other1Id() {
			return AbstractClusterLinkTest.SELF_ID + 1;
		}

		@Override
		protected long other2Id() {
			return AbstractClusterLinkTest.SELF_ID - 1;
		}

		@Override
		protected long other3Id() {
			return AbstractClusterLinkTest.SELF_ID - 2;
		}

		@Override
		protected AgentType other1Type() {
			return AgentType.EVENT_PROCESSING_STATIC_SHARDING;
		}

		@Override
		protected AgentType selfType() {
			return AgentType.EVENT_PROCESSING_STATIC_SHARDING;
		}

		@Override
		protected AgentType other2Type() {
			return AgentType.EVENT_PROCESSING_STATIC_SHARDING;
		}

		@Override
		protected AgentType other3Type() {
			return AgentType.EVENT_PROCESSING_STATIC_SHARDING;
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
			return expectSuspendedAndPulseASAP();
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectRebalancing( selfShardAssignmentIn4NodeCluster() );
		}
	}

	public static class SuspendedTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( EventProcessingState.SUSPENDED, selfStaticShardAssignment() );
		}

		@Override
		protected ClusterLinkPulseExpectations onNoOtherAgents() {
			return expectSuspendedAndPulseASAP();
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
			return expectSuspendedAndPulseASAP();
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
			return expectSuspendedAndPulseASAP();
		}

		@Override
		protected ClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			// We were already running and had the correct cluster => we can run now!
			return expectRunning( selfShardAssignmentIn4NodeCluster() );
		}
	}

}
