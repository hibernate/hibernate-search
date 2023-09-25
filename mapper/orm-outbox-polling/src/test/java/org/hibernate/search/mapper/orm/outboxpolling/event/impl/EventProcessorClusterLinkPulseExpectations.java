/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.mapper.orm.outboxpolling.event.impl.AbstractEventProcessorClusterLinkTest.NOW;
import static org.hibernate.search.mapper.orm.outboxpolling.event.impl.AbstractEventProcessorClusterLinkTest.PULSE_EXPIRATION;
import static org.hibernate.search.mapper.orm.outboxpolling.event.impl.AbstractEventProcessorClusterLinkTest.PULSE_INTERVAL;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;

public class EventProcessorClusterLinkPulseExpectations {

	public static InstructionsStep expect(AgentRepositoryMockingHelper repoMockingHelper,
			OutboxEventFinder eventFinderMock,
			ShardAssignmentDescriptor selfStaticShardAssignment,
			OutboxPollingEventProcessorClusterLink link) {
		return new Builder( repoMockingHelper, eventFinderMock, selfStaticShardAssignment, link );
	}

	private final AgentRepositoryMockingHelper repoMockingHelper;
	private final OutboxPollingEventProcessorClusterLink link;

	private final ShardAssignmentDescriptor expectedLinkShardAssignment;

	private final Instant expectedInstructionsExpiration;
	private final Optional<OutboxEventFinder> expectedInstructionsEventFinder;

	private final UUID expectedSelfAgentId;
	private final Instant expectedSelfAgentExpiration;
	private final AgentState expectedSelfAgentCurrentState;
	private final ShardAssignmentDescriptor expectedSelfAgentShardAssignment;

	private EventProcessorClusterLinkPulseExpectations(Builder builder) {
		this.repoMockingHelper = builder.repoMockingHelper;
		this.link = builder.link;
		this.expectedLinkShardAssignment = builder.expectedLinkShardAssignment;
		this.expectedInstructionsExpiration = builder.expectedInstructionsExpiration;
		this.expectedInstructionsEventFinder = builder.expectedInstructionsEventFinder;
		this.expectedSelfAgentId = builder.expectedSelfAgentId;
		this.expectedSelfAgentExpiration = builder.expectedSelfAgentExpiration;
		this.expectedSelfAgentCurrentState = builder.expectedSelfAgentCurrentState;
		this.expectedSelfAgentShardAssignment = builder.expectedSelfAgentShardAssignment;
	}

	@Override
	public String toString() {
		return "ClusterLinkPulseExpectations{" +
				"expectedLinkShardAssignment=" + expectedLinkShardAssignment +
				", expectedInstructionsExpiration=" + expectedInstructionsExpiration +
				", expectedInstructionsEventFinder=" + expectedInstructionsEventFinder +
				", expectedSelfAgentId=" + expectedSelfAgentId +
				", expectedSelfAgentExpiration=" + expectedSelfAgentExpiration +
				", expectedSelfAgentCurrentState=" + expectedSelfAgentCurrentState +
				", expectedSelfAgentShardAssignment=" + expectedSelfAgentShardAssignment +
				'}';
	}

	void verify(OutboxPollingEventProcessingInstructions instructions) {
		assertSoftly( softly -> {
			if ( expectedLinkShardAssignment != null ) {
				softly.assertThat( link.lastShardAssignment )
						.as( "link.lastShardAssignment" )
						.isNotNull()
						.extracting( a -> a == null ? null : a.descriptor )
						.as( "link.lastShardAssignment.descriptor" )
						.isEqualTo( expectedLinkShardAssignment );
			}

			softly.assertThat( instructions.expiration )
					.as( "instructions.expiration" )
					.isEqualTo( expectedInstructionsExpiration );
			softly.assertThat( instructions.eventFinder )
					.as( "instructions.eventFinder" )
					.isEqualTo( expectedInstructionsEventFinder );

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
					.isEqualTo( expectedSelfAgentShardAssignment );
		} );
	}

	public interface InstructionsStep {
		AgentMainStateStep pulseAgain(Instant expiration);

		AgentMainStateStep processThenPulse(ShardAssignmentDescriptor expectedLinkShardAssignment);
	}

	public interface AgentMainStateStep {
		AgentOptionsStep agent(UUID expectedId, AgentState expectedCurrentState);
	}

	public interface AgentOptionsStep {
		AgentOptionsStep expiration(Instant instant);

		AgentOptionsStep shardAssignment(ShardAssignmentDescriptor shardAssignment);

		EventProcessorClusterLinkPulseExpectations build();
	}

	private static class Builder implements InstructionsStep, AgentMainStateStep, AgentOptionsStep {
		private final AgentRepositoryMockingHelper repoMockingHelper;
		private final OutboxEventFinder eventFinderMock;
		private final OutboxPollingEventProcessorClusterLink link;

		private ShardAssignmentDescriptor expectedLinkShardAssignment;

		private Instant expectedInstructionsExpiration;
		private Optional<OutboxEventFinder> expectedInstructionsEventFinder;

		private UUID expectedSelfAgentId;
		private Instant expectedSelfAgentExpiration;
		private AgentState expectedSelfAgentCurrentState;
		private ShardAssignmentDescriptor expectedSelfAgentShardAssignment;

		private Builder(AgentRepositoryMockingHelper repoMockingHelper,
				OutboxEventFinder eventFinderMock,
				ShardAssignmentDescriptor selfStaticShardAssignment,
				OutboxPollingEventProcessorClusterLink link) {
			this.repoMockingHelper = repoMockingHelper;
			this.eventFinderMock = eventFinderMock;
			this.link = link;
			this.expectedLinkShardAssignment = selfStaticShardAssignment;
		}

		@Override
		public AgentMainStateStep pulseAgain(Instant expiration) {
			expectedInstructionsExpiration = expiration;
			expectedInstructionsEventFinder = Optional.empty();
			return this;
		}

		@Override
		public AgentMainStateStep processThenPulse(ShardAssignmentDescriptor expectedLinkShardAssignment) {
			this.expectedLinkShardAssignment = expectedLinkShardAssignment;
			expectedInstructionsExpiration = NOW.plus( PULSE_INTERVAL );
			expectedInstructionsEventFinder = Optional.of( eventFinderMock );
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
		public AgentOptionsStep shardAssignment(ShardAssignmentDescriptor shardAssignment) {
			this.expectedSelfAgentShardAssignment = shardAssignment;
			return this;
		}

		@Override
		public EventProcessorClusterLinkPulseExpectations build() {
			return new EventProcessorClusterLinkPulseExpectations( this );
		}
	}

}
