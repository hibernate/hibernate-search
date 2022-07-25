/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Base tests of {@link OutboxPollingEventProcessorClusterLink}
 * with dynamic sharding.
 */
@RunWith(NestedRunner.class)
public class EventProcessorClusterLinkDynamicShardingBaseTest {

	abstract static class AbstractBaseTest extends AbstractEventProcessorClusterLinkBaseTest {
		// Define IDs in ascending order, because IDs matter when using dynamic sharding.

		@Override
		protected long other1Id() {
			return AbstractEventProcessorClusterLinkTest.SELF_ID - 1;
		}

		@Override
		protected long other2Id() {
			return AbstractEventProcessorClusterLinkTest.SELF_ID + 1;
		}

		@Override
		protected long other3Id() {
			return AbstractEventProcessorClusterLinkTest.SELF_ID + 2;
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
		public void clusterWith4Nodes_allWaiting_clusterExcludingSelf() {
			repositoryMockHelper.defineOtherAgents()
					.other( other1Id(), other1Type(), LATER, AgentState.WAITING,
							new ShardAssignmentDescriptor( 3, 0 ) )
					.other( other2Id(), other2Type(), LATER, AgentState.WAITING,
							new ShardAssignmentDescriptor( 3, 1 ) )
					.other( other3Id(), other3Type(), LATER, AgentState.WAITING,
							new ShardAssignmentDescriptor( 3, 2 ) );

			expectWaiting( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( contextMock ) );
		}

		@Test
		public void clusterWith4Nodes_allRunning_clusterExcludingSelf() {
			repositoryMockHelper.defineOtherAgents()
					.other( other1Id(), other1Type(), LATER, AgentState.RUNNING,
							new ShardAssignmentDescriptor( 3, 0 ) )
					.other( other2Id(), other2Type(), LATER, AgentState.RUNNING,
							new ShardAssignmentDescriptor( 3, 1 ) )
					.other( other3Id(), other3Type(), LATER, AgentState.RUNNING,
							new ShardAssignmentDescriptor( 3, 2 ) );

			expectWaiting( selfShardAssignmentIn4NodeCluster() ).verify( link.pulse( contextMock ) );
		}
	}

	// It's very important that an agent that wasn't registered
	// must not start running immediately on the first pulse,
	// but should go through the waiting state first.
	// See comments in OutboxPollingEventProcessorClusterLink.
	@Nested
	public static class NotRegisteredTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfNotCreatedYet();
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectWaiting( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class SuspendedTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.SUSPENDED, null );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectWaiting( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class WaitingIn1NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.WAITING, selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			// We were already waiting and had the correct cluster => we can run now!
			return expectRunning( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectWaiting( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class RunningIn1NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.RUNNING, selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectRunning( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectWaiting( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class WaitingIn3NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.WAITING, selfShardAssignmentIn3NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectWaiting( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class RunningIn3NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.RUNNING, selfShardAssignmentIn3NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectWaiting( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class WaitingIn4NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.WAITING, selfShardAssignmentIn4NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			// We were already waiting and had the correct cluster => we can run now!
			return expectRunning( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class RunningIn4NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.RUNNING, selfShardAssignmentIn4NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			// We were already running and had the correct cluster => we can run now!
			return expectRunning( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class WaitingIn5NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.WAITING, shardAssignmentIn5NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectWaiting( selfShardAssignmentIn4NodeCluster() );
		}
	}

	@Nested
	public static class RunningIn5NodeClusterTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.RUNNING, shardAssignmentIn5NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting( selfShardAssignmentIn1NodeCluster() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			return expectWaiting( selfShardAssignmentIn4NodeCluster() );
		}
	}

}
