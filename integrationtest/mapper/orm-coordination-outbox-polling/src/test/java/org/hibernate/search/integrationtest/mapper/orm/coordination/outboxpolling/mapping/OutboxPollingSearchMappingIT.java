/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.OutboxPollingExtension;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.OutboxPollingSearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OutboxPollingSearchMappingIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;
	private OutboxPollingSearchMapping searchMapping;
	private AbortedEventsGenerator abortedEventsGenerator;

	@Before
	public void before() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b.field( "indexedField", String.class ) );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.event_processor.retry_delay", 0 )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
		searchMapping = Search.mapping( sessionFactory ).extension( OutboxPollingExtension.get() );
		abortedEventsGenerator = new AbortedEventsGenerator( sessionFactory, backendMock );
	}

	@Test
	public void countAbortedEvents_tenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.countAbortedEvents( "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is not enabled but a tenant id is specified.",
						"Trying to use the tenant id: 'tenantX'."
				);
	}

	@Test
	public void reprocessAbortedEvents_tenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.reprocessAbortedEvents( "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is not enabled but a tenant id is specified.",
						"Trying to use the tenant id: 'tenantX'."
				);
	}

	@Test
	public void clearAllAbortedEvents_tenantIdSpecified() {
		assertThatThrownBy( () -> searchMapping.clearAllAbortedEvents( "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is not enabled but a tenant id is specified.",
						"Trying to use the tenant id: 'tenantX'."
				);
	}

	@Test
	public void clearAllAbortedEvents() {
		assertThat( searchMapping.countAbortedEvents() ).isZero();

		abortedEventsGenerator.generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents() ).isEqualTo( 3 );

		assertThat( searchMapping.clearAllAbortedEvents() ).isEqualTo( 3 );

		assertThat( searchMapping.countAbortedEvents() ).isZero();
	}

	@Test
	public void reprocessAbortedEvents() {
		assertThat( searchMapping.countAbortedEvents() ).isZero();

		abortedEventsGenerator.generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents() ).isEqualTo( 3 );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.createAndExecuteFollowingWorks()
				.addOrUpdate( "1", b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( "2", b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( "3", b -> b
						.field( "indexedField", "initialValue" )
				);
		assertThat( searchMapping.reprocessAbortedEvents() ).isEqualTo( 3 );
		backendMock.verifyExpectationsMet();

		assertThat( searchMapping.countAbortedEvents() ).isZero();
	}
}
