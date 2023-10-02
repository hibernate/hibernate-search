/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;

import org.mockito.ArgumentCaptor;

class AgentRepositoryMockingHelper {

	private final AgentRepository repositoryMock;

	private final List<Agent> otherAgents = new ArrayList<>();
	private Supplier<Agent> selfSupplier = null;
	private BooleanSupplier selfExists;
	private AgentState selfInitialState;
	private Instant selfInitialExpiration;
	private ShardAssignmentDescriptor selfInitialShardAssignment;

	public AgentRepositoryMockingHelper(AgentRepository repositoryMock) {
		this.repositoryMock = repositoryMock;
	}

	void defineSelfCreatedByPulse(UUID selfId) {
		ArgumentCaptor<Agent> selfCaptor = ArgumentCaptor.forClass( Agent.class );
		selfSupplier = selfCaptor::getValue;
		selfExists = () -> !selfCaptor.getAllValues().isEmpty();
		selfInitialState = null;
		selfInitialExpiration = null;
		selfInitialShardAssignment = null;
		doAnswer( invocation -> {
			Agent agent = invocation.getArgument( 0 );
			agent.setId( selfId );
			return agent;
		} )
				.when( repositoryMock ).create( selfCaptor.capture() );
		doAnswer( invocation -> selfExists.getAsBoolean() ? selfSupplier.get() : null )
				.when( repositoryMock ).find( selfId );
	}

	void defineSelfPreExisting(Agent self) {
		selfSupplier = () -> self;
		selfExists = () -> true;
		selfInitialState = self.getState();
		selfInitialExpiration = self.getExpiration();
		selfInitialShardAssignment = self.getShardAssignment();
		doReturn( self ).when( repositoryMock ).find( self.getId() );
	}

	AllAgentsDefinition defineOtherAgents() {
		otherAgents.clear();
		return new AllAgentsDefinition();
	}

	Agent self() {
		if ( selfSupplier == null ) {
			throw new Error(
					"You must call either defineSelfCreatedByPulse() or defineSelfPreExisting() before the test" );
		}
		return selfSupplier.get();
	}

	List<Agent> allAgentsInIdOrder() {
		return Stream
				.concat( otherAgents.stream(), selfExists.getAsBoolean() ? Stream.of( selfSupplier.get() ) : Stream.empty() )
				.sorted( Comparator.comparing( Agent::getId ) )
				.collect( Collectors.toList() );
	}

	List<Agent> agentsInIdOrder(UUID... ids) {
		Set<UUID> idSet = new HashSet<>( Arrays.asList( ids ) );
		return allAgentsInIdOrder().stream()
				.filter( agent -> idSet.contains( agent.getId() ) )
				.collect( Collectors.toList() );
	}

	public AgentState selfInitialState() {
		return selfInitialState;
	}

	public Instant selfInitialExpiration() {
		return selfInitialExpiration;
	}

	public ShardAssignmentDescriptor selfInitialShardAssignment() {
		return selfInitialShardAssignment;
	}

	class AllAgentsDefinition {
		AllAgentsDefinition other(UUID id, AgentType type, Instant expiration, AgentState state) {
			return other( id, type, expiration, state, null );
		}

		AllAgentsDefinition other(UUID id, AgentType type, Instant expiration, AgentState state,
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
