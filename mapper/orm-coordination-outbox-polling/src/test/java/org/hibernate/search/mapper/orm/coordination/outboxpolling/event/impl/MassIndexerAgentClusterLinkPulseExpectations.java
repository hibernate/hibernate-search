/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.AbstractMassIndexerAgentClusterLinkTest.NOW;
import static org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.AbstractMassIndexerAgentClusterLinkTest.PULSE_EXPIRATION;
import static org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.AbstractMassIndexerAgentClusterLinkTest.PULSE_INTERVAL;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;

public class MassIndexerAgentClusterLinkPulseExpectations {

	public static InstructionsStep expect(AgentRepositoryMockingHelper repoMockingHelper,
			OutboxPollingMassIndexerAgentClusterLink link) {
		return new Builder( repoMockingHelper, link );
	}

	private final AgentRepositoryMockingHelper repoMockingHelper;
	private final OutboxPollingMassIndexerAgentClusterLink link;

	private final Instant expectedInstructionsExpiration;
	private final boolean expectedInstructionsConsiderEventProcessingSuspended;

	private final UUID expectedSelfAgentId;
	private final Instant expectedSelfAgentExpiration;
	private final AgentState expectedSelfAgentCurrentState;

	private MassIndexerAgentClusterLinkPulseExpectations(Builder builder) {
		this.repoMockingHelper = builder.repoMockingHelper;
		this.link = builder.link;
		this.expectedInstructionsExpiration = builder.expectedInstructionsExpiration;
		this.expectedInstructionsConsiderEventProcessingSuspended = builder.expectedInstructionsConsiderEventProcessingSuspended;
		this.expectedSelfAgentId = builder.expectedSelfAgentId;
		this.expectedSelfAgentExpiration = builder.expectedSelfAgentExpiration;
		this.expectedSelfAgentCurrentState = builder.expectedSelfAgentCurrentState;
	}

	@Override
	public String toString() {
		return "ClusterLinkPulseExpectations{" +
				", expectedInstructionsExpiration=" + expectedInstructionsExpiration +
				", expectedInstructionsConsiderEventProcessingSuspended=" + expectedInstructionsConsiderEventProcessingSuspended +
				", expectedSelfAgentId=" + expectedSelfAgentId +
				", expectedSelfAgentExpiration=" + expectedSelfAgentExpiration +
				", expectedSelfAgentCurrentState=" + expectedSelfAgentCurrentState +
				'}';
	}

	void verify(OutboxPollingMassIndexingInstructions instructions) {
		assertSoftly( softly -> {
			softly.assertThat( instructions.expiration )
					.as( "instructions.expiration" )
					.isEqualTo( expectedInstructionsExpiration );
			softly.assertThat( instructions.considerEventProcessingSuspended )
					.as( "instructions.considerEventProcessingSuspended" )
					.isEqualTo( expectedInstructionsConsiderEventProcessingSuspended );

			Agent selfAgent = repoMockingHelper.self();
			softly.assertThat( selfAgent.getId() )
					.as( "selfAgent.id" )
					.isEqualTo( expectedSelfAgentId );
			softly.assertThat( selfAgent.getExpiration() )
					.as( "selfAgent.expiration" )
					.isEqualTo( expectedSelfAgentExpiration );
			softly.assertThat( selfAgent.getState() )
					.as( "selfAgent.state" )
					.isEqualTo( expectedSelfAgentCurrentState );
			softly.assertThat( selfAgent.getShardAssignment() )
					.as( "selfAgent.shardAssignment" )
					.isNull();
		} );
	}

	public interface InstructionsStep {
		AgentMainStateStep pulseAgain(Instant expiration);

		AgentMainStateStep considerEventProcessingSuspendedThenPulse();
	}

	public interface AgentMainStateStep {
		AgentOptionsStep agent(UUID expectedId, AgentState expectedCurrentState);
	}

	public interface AgentOptionsStep {
		AgentOptionsStep expiration(Instant instant);

		MassIndexerAgentClusterLinkPulseExpectations build();
	}

	private static class Builder implements InstructionsStep, AgentMainStateStep, AgentOptionsStep {
		private final AgentRepositoryMockingHelper repoMockingHelper;
		private final OutboxPollingMassIndexerAgentClusterLink link;

		private Instant expectedInstructionsExpiration;
		private boolean expectedInstructionsConsiderEventProcessingSuspended;

		private UUID expectedSelfAgentId;
		private Instant expectedSelfAgentExpiration;
		private AgentState expectedSelfAgentCurrentState;

		private Builder(AgentRepositoryMockingHelper repoMockingHelper,
				OutboxPollingMassIndexerAgentClusterLink link) {
			this.repoMockingHelper = repoMockingHelper;
			this.link = link;
		}

		@Override
		public AgentMainStateStep pulseAgain(Instant expiration) {
			expectedInstructionsExpiration = expiration;
			expectedInstructionsConsiderEventProcessingSuspended = false;
			return this;
		}

		@Override
		public AgentMainStateStep considerEventProcessingSuspendedThenPulse() {
			expectedInstructionsExpiration = NOW.plus( PULSE_INTERVAL );
			expectedInstructionsConsiderEventProcessingSuspended = true;
			return this;
		}

		@Override
		public AgentOptionsStep agent(UUID expectedId, AgentState expectedCurrentState) {
			this.expectedSelfAgentId = expectedId;
			this.expectedSelfAgentCurrentState = expectedCurrentState;
			this.expectedSelfAgentExpiration = NOW.plus( PULSE_EXPIRATION );
			return this;
		}

		@Override
		public AgentOptionsStep expiration(Instant instant) {
			this.expectedSelfAgentExpiration = instant;
			return this;
		}

		@Override
		public MassIndexerAgentClusterLinkPulseExpectations build() {
			return new MassIndexerAgentClusterLinkPulseExpectations( this );
		}
	}

}
