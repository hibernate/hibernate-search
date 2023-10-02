/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.util.UUID;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Base tests of {@link OutboxPollingEventProcessorClusterLink}
 * with dynamic sharding.
 */
class EventProcessorClusterLinkDynamicShardingBaseTest {

	abstract static class AbstractBaseTest extends AbstractEventProcessorClusterLinkBaseTest {
		// Define IDs in ascending order, because IDs matter when using dynamic sharding.

		@Override
		protected UUID other1Id() {
			return toUUID( SELF_ID_ORDINAL - 1 );
		}

		@Override
		protected UUID other2Id() {
			return toUUID( SELF_ID_ORDINAL + 1 );
		}

		@Override
		protected UUID other3Id() {
			return toUUID( SELF_ID_ORDINAL + 2 );
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
	public class NotRegisteredTest extends AbstractBaseTest {
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
	public class SuspendedTest extends AbstractBaseTest {
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
	public class WaitingIn1NodeClusterTest extends AbstractBaseTest {
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
	public class RunningIn1NodeClusterTest extends AbstractBaseTest {
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
	public class WaitingIn3NodeClusterTest extends AbstractBaseTest {
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
	public class RunningIn3NodeClusterTest extends AbstractBaseTest {
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
	public class WaitingIn4NodeClusterTest extends AbstractBaseTest {
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
	public class RunningIn4NodeClusterTest extends AbstractBaseTest {
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
	public class WaitingIn5NodeClusterTest extends AbstractBaseTest {
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
	public class RunningIn5NodeClusterTest extends AbstractBaseTest {
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
