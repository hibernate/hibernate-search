/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.EventProcessingState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;

import org.mockito.ArgumentCaptor;

class AgentRepositoryMockingHelper {

	private final AgentRepository repositoryMock;

	private final List<Agent> otherAgents = new ArrayList<>();
	private Supplier<Agent> selfSupplier = null;
	private boolean selfPreExisting;
	private EventProcessingState selfInitialState;
	private Instant selfInitialExpiration;
	private ShardAssignmentDescriptor selfInitialShardAssignment;

	public AgentRepositoryMockingHelper(AgentRepository repositoryMock) {
		this.repositoryMock = repositoryMock;
	}

	void defineSelfCreatedByPulse(long selfId) {
		ArgumentCaptor<Agent> selfCaptor = ArgumentCaptor.forClass( Agent.class );
		selfSupplier = selfCaptor::getValue;
		selfPreExisting = false;
		selfInitialState = null;
		selfInitialExpiration = null;
		selfInitialShardAssignment = null;
		doAnswer( invocation -> {
			Agent agent = invocation.getArgument( 0 );
			agent.setId( selfId );
			return agent;
		} )
				.when( repositoryMock ).create( selfCaptor.capture() );
	}

	void defineSelfPreExisting(Agent self) {
		selfSupplier = () -> self;
		selfPreExisting = true;
		selfInitialState = self.getState();
		selfInitialExpiration = self.getExpiration();
		selfInitialShardAssignment = self.getShardAssignment();
	}

	AllAgentsDefinition defineOtherAgents() {
		otherAgents.clear();
		return new AllAgentsDefinition();
	}

	Agent self() {
		if ( selfSupplier == null ) {
			throw new Error(
					"You must call either expectSelfCreatedByPulse() or expectSelfPreExisting() before the test" );
		}
		return selfSupplier.get();
	}

	List<Agent> allAgentsInIdOrder() {
		return Stream.concat( otherAgents.stream(), selfPreExisting ? Stream.of( self() ) : Stream.empty() )
				.sorted( Comparator.comparing( Agent::getId ) )
				.collect( Collectors.toList() );
	}

	List<Agent> agentsInIdOrder(Long ... ids) {
		Set<Long> idSet = new HashSet<>( Arrays.asList( ids ) );
		return allAgentsInIdOrder().stream()
				.filter( agent -> idSet.contains( agent.getId() ) )
				.collect( Collectors.toList() );
	}

	public EventProcessingState selfInitialState() {
		return selfInitialState;
	}

	public Instant selfInitialExpiration() {
		return selfInitialExpiration;
	}

	public ShardAssignmentDescriptor selfInitialShardAssignment() {
		return selfInitialShardAssignment;
	}

	class AllAgentsDefinition {
		AllAgentsDefinition other(Long id, AgentType type, Instant expiration, EventProcessingState state) {
			return other( id, type, expiration, state, null );
		}

		AllAgentsDefinition other(Long id, AgentType type, Instant expiration, EventProcessingState state,
				ShardAssignmentDescriptor shardAssignment) {
			Agent agent = new Agent( type, "other agent", expiration, state, shardAssignment );
			agent.setId( id );
			Agent spy = mock( Agent.class, withSettings()
					.spiedInstance( agent )
					.defaultAnswer( CALLS_REAL_METHODS )
					.stubbingLookupListeners( event -> {
						if ( event.getInvocation().getMethod().getName().startsWith( "set" ) ) {
							fail( "Illegal invocation of a setter on an agent other than self" );
						}
					} ) );
			otherAgents.add( spy );
			return this;
		}
	}

}
