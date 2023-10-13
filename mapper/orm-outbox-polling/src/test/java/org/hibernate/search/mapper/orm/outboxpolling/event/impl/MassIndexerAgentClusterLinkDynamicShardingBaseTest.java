/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.hibernate.search.mapper.orm.outboxpolling.event.impl.AbstractEventProcessorClusterLinkTest.toUUID;

import java.util.UUID;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;

import org.junit.jupiter.api.Nested;

/**
 * Base tests of {@link OutboxPollingMassIndexerAgentClusterLink}
 * with event processors using dynamic sharding.
 */
class MassIndexerAgentClusterLinkDynamicShardingBaseTest {

	abstract static class AbstractBaseTest extends AbstractMassIndexerAgentClusterLinkBaseTest {

		// Define IDs of statically agents in the opposite order of the shard order,
		// to demonstrate that the ID order doesn't matter when using static sharding:

		@Override
		protected UUID other1Id() {
			return toUUID( SELF_ID_ORDINAL + 1 );
		}

		@Override
		protected UUID other2Id() {
			return toUUID( SELF_ID_ORDINAL - 1 );
		}

		@Override
		protected UUID other3Id() {
			return toUUID( SELF_ID_ORDINAL - 2 );
		}

		@Override
		protected AgentType otherType() {
			return AgentType.EVENT_PROCESSING_STATIC_SHARDING;
		}

	}

	// It's very important that an agent that wasn't registered
	// must not start running immediately on the first pulse,
	// but should go through the waiting state first.
	// See comments in OutboxPollingMassIndexerAgentClusterLink.
	@Nested
	public class NotRegisteredTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfNotCreatedYet();
		}

		@Override
		protected MassIndexerAgentClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting();
		}

		@Override
		protected MassIndexerAgentClusterLinkPulseExpectations onClusterWith3NodesAll3NodesSuspended() {
			return expectWaiting();
		}
	}

	@Nested
	public class SuspendedTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.SUSPENDED );
		}

		@Override
		protected MassIndexerAgentClusterLinkPulseExpectations onNoOtherAgents() {
			return expectWaiting();
		}

		@Override
		protected MassIndexerAgentClusterLinkPulseExpectations onClusterWith3NodesAll3NodesSuspended() {
			return expectWaiting();
		}
	}

	@Nested
	public class WaitingTest extends AbstractBaseTest {

		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.WAITING );
		}

		@Override
		protected MassIndexerAgentClusterLinkPulseExpectations onNoOtherAgents() {
			// We were already waiting => we can run now!
			return expectRunning();
		}

		@Override
		protected MassIndexerAgentClusterLinkPulseExpectations onClusterWith3NodesAll3NodesSuspended() {
			// We were already waiting => we can run now!
			return expectRunning();
		}
	}

	@Nested
	public class RunningTest extends AbstractBaseTest {
		@Override
		protected void defineSelf() {
			defineSelfCreatedAndStillPresent( AgentState.RUNNING );
		}

		@Override
		protected MassIndexerAgentClusterLinkPulseExpectations onNoOtherAgents() {
			// We were already running => we can run now!
			return expectRunning();
		}

		@Override
		protected MassIndexerAgentClusterLinkPulseExpectations onClusterWith3NodesAll3NodesSuspended() {
			// We were already running => we can run now!
			return expectRunning();
		}
	}

}
