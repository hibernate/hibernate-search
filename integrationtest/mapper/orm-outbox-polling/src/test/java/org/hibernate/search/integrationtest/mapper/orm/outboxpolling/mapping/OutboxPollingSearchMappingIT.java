/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.outboxpolling.OutboxPollingExtension;
import org.hibernate.search.mapper.orm.outboxpolling.mapping.OutboxPollingSearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OutboxPollingSearchMappingIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	private SessionFactory sessionFactory;
	private OutboxPollingSearchMapping searchMapping;
	private AbortedEventsGenerator abortedEventsGenerator;

	@BeforeEach
	void before() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b.field( "indexedField", String.class ) );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.event_processor.retry_delay", 0 )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
		searchMapping = Search.mapping( sessionFactory ).extension( OutboxPollingExtension.get() );
		abortedEventsGenerator = new AbortedEventsGenerator( sessionFactory, backendMock );
	}

	@Test
	void countAbortedEvents_tenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.countAbortedEvents( (Object) "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is not enabled but a tenant id is specified.",
						"Trying to use the tenant id: 'tenantX'."
				);
	}

	@Test
	void reprocessAbortedEvents_tenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.reprocessAbortedEvents( (Object) "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is not enabled but a tenant id is specified.",
						"Trying to use the tenant id: 'tenantX'."
				);
	}

	@Test
	void clearAllAbortedEvents_tenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.clearAllAbortedEvents( (Object) "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is not enabled but a tenant id is specified.",
						"Trying to use the tenant id: 'tenantX'."
				);
	}

	@Test
	void clearAllAbortedEvents() {
		assertThat( searchMapping.countAbortedEvents() ).isZero();

		abortedEventsGenerator.generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents() ).isEqualTo( 3 );

		assertThat( searchMapping.clearAllAbortedEvents() ).isEqualTo( 3 );

		assertThat( searchMapping.countAbortedEvents() ).isZero();
	}

	@Test
	void reprocessAbortedEvents() {
		assertThat( searchMapping.countAbortedEvents() ).isZero();

		List<Integer> generatedIDs = abortedEventsGenerator.generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents() ).isEqualTo( 3 );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.createAndExecuteFollowingWorks()
				.addOrUpdate( Integer.toString( generatedIDs.get( 0 ) ), b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( Integer.toString( generatedIDs.get( 1 ) ), b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( Integer.toString( generatedIDs.get( 2 ) ), b -> b
						.field( "indexedField", "initialValue" )
				);
		assertThat( searchMapping.reprocessAbortedEvents() ).isEqualTo( 3 );
		backendMock.verifyExpectationsMet();

		assertThat( searchMapping.countAbortedEvents() ).isZero();
	}
}
