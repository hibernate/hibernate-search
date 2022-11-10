/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.util.UUID;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.runner.RunWith;

/**
 * Base tests of {@link OutboxPollingEventProcessorClusterLink}
 * with static sharding with a total shard count of 4.
 */
@RunWith(NestedRunner.class)
public class EventProcessorClusterLinkStaticSharding4ShardBaseTest {

	abstract static class AbstractBaseTest extends AbstractEventProcessorClusterLinkBaseTest {

		// Define IDs of statically agents in the opposite order of the shard order,
		// to demonstrate that the ID order doesn't matter when using static sharding:

		@Override
		protected UUID other1Id() {
			return toUUID( AbstractEventProcessorClusterLinkTest.SELF_ID_ORDINAL + 1 );
		}

		@Override
		protected UUID other2Id() {
			return toUUID( AbstractEventProcessorClusterLinkTest.SELF_ID_ORDINAL - 1 );
		}

		@Override
		protected UUID other3Id() {
			return toUUID( AbstractEventProcessorClusterLinkTest.SELF_ID_ORDINAL - 2 );
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
			return expectSuspendedAndPulseAfterDelay( POLLING_INTERVAL );
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
			defineSelfCreatedAndStillPresent( AgentState.SUSPENDED, selfStaticShardAssignment() );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onNoOtherAgents() {
			return expectSuspendedAndPulseAfterDelay( POLLING_INTERVAL );
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
			return expectSuspendedAndPulseAfterDelay( POLLING_INTERVAL );
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
			return expectSuspendedAndPulseAfterDelay( POLLING_INTERVAL );
		}

		@Override
		protected EventProcessorClusterLinkPulseExpectations onClusterWith4NodesAllOther3NodesReady() {
			// We were already running and had the correct cluster => we can run now!
			return expectRunning( selfShardAssignmentIn4NodeCluster() );
		}
	}

}
