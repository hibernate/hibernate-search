/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.outboxpolling.OutboxPollingExtension;
import org.hibernate.search.mapper.orm.outboxpolling.mapping.OutboxPollingSearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OutboxPollingSearchMappingMultiTenancyIT {

	private static final Object TENANT_1_ID = "tenant1";
	private static final Object TENANT_2_ID = "tenant2";
	private static final Object TENANT_3_ID = "tenant3";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of(
						IndexedMultiTenantEntity.INDEX,
						IndexedMultiTenantEntity.class,
						(BiConsumer<Session, Integer>) (session, id) -> {
							IndexedMultiTenantEntity entity1 = new IndexedMultiTenantEntity();
							entity1.setId( id );
							entity1.setIndexedField( "initialValue" );
							session.persist( entity1 );
						},
						(Consumer<OrmSetupHelper.SetupContext>) context -> context.tenants( false, TENANT_1_ID, TENANT_2_ID,
								TENANT_3_ID )
				),
				Arguments.of(
						IndexedEntity.INDEX,
						IndexedEntity.class,
						(BiConsumer<Session, Integer>) (session, id) -> {
							IndexedEntity entity1 = new IndexedEntity();
							entity1.setId( id );
							entity1.setIndexedField( "initialValue" );
							session.persist( entity1 );
						},
						(Consumer<OrmSetupHelper.SetupContext>) context -> {
							context.tenantsWithHelperEnabled( TENANT_1_ID, TENANT_2_ID, TENANT_3_ID );
						}
				)
		);
	}

	private SessionFactory sessionFactory;
	private OutboxPollingSearchMapping searchMapping;
	private AbortedEventsGenerator abortedEventsGenerator1;
	private AbortedEventsGenerator abortedEventsGenerator2;

	void init(String indexName, Class<?> indexedEntity, BiConsumer<Session, Integer> entityCreator,
			Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		backendMock.expectSchema( indexName, b -> b.field( "indexedField", String.class ) );
		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start();
		tenantConfigurer.accept( setupContext );
		sessionFactory = setupContext
				.withProperty( "hibernate.search.coordination.event_processor.retry_delay", 0 )
				.setup( indexedEntity );
		backendMock.verifyExpectationsMet();
		searchMapping = Search.mapping( sessionFactory ).extension( OutboxPollingExtension.get() );
		abortedEventsGenerator1 =
				new AbortedEventsGenerator( sessionFactory, backendMock, TENANT_1_ID, indexName, entityCreator );
		abortedEventsGenerator2 =
				new AbortedEventsGenerator( sessionFactory, backendMock, TENANT_2_ID, indexName, entityCreator );
	}

	@ParameterizedTest(name = "entity = {0}")
	@MethodSource("params")
	void ountAbortedEvents_noTenantIdSpecified(String indexName, Class<?> indexedEntity,
			BiConsumer<Session, Integer> entityCreator, Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		init( indexName, indexedEntity, entityCreator, tenantConfigurer );
		assertThatThrownBy( () -> searchMapping.countAbortedEvents() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is enabled but no tenant id is specified.",
						"Available tenants are: '[tenant1, tenant2, tenant3]'."
				);
	}

	@ParameterizedTest(name = "entity = {0}")
	@MethodSource("params")
	void countAbortedEvents_wrongTenantId(String indexName, Class<?> indexedEntity,
			BiConsumer<Session, Integer> entityCreator, Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		init( indexName, indexedEntity, entityCreator, tenantConfigurer );
		assertThatThrownBy( () -> searchMapping.countAbortedEvents( (Object) "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot target tenant 'tenantX'",
						"Currently configured tenant identifiers: [tenant1, tenant2, tenant3]."
				);
	}

	@ParameterizedTest(name = "entity = {0}")
	@MethodSource("params")
	void reprocessAbortedEvents_noTenantIdSpecified(String indexName, Class<?> indexedEntity,
			BiConsumer<Session, Integer> entityCreator, Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		init( indexName, indexedEntity, entityCreator, tenantConfigurer );
		assertThatThrownBy( () -> searchMapping.reprocessAbortedEvents() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is enabled but no tenant id is specified.",
						"Available tenants are: '[tenant1, tenant2, tenant3]'."
				);
	}

	@ParameterizedTest(name = "entity = {0}")
	@MethodSource("params")
	void reprocessAbortedEvents_wrongTenantId(String indexName, Class<?> indexedEntity,
			BiConsumer<Session, Integer> entityCreator, Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		init( indexName, indexedEntity, entityCreator, tenantConfigurer );
		assertThatThrownBy( () -> searchMapping.reprocessAbortedEvents( (Object) "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot target tenant 'tenantX'",
						"Currently configured tenant identifiers: [tenant1, tenant2, tenant3]."
				);
	}

	@ParameterizedTest(name = "entity = {0}")
	@MethodSource("params")
	void clearAllAbortedEvents_noTenantIdSpecified(String indexName, Class<?> indexedEntity,
			BiConsumer<Session, Integer> entityCreator, Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		init( indexName, indexedEntity, entityCreator, tenantConfigurer );
		assertThatThrownBy( () -> searchMapping.clearAllAbortedEvents() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Multi-tenancy is enabled but no tenant id is specified.",
						"Available tenants are: '[tenant1, tenant2, tenant3]'."
				);
	}

	@ParameterizedTest(name = "entity = {0}")
	@MethodSource("params")
	void clearAllAbortedEvents_wrongTenantId(String indexName, Class<?> indexedEntity,
			BiConsumer<Session, Integer> entityCreator, Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		init( indexName, indexedEntity, entityCreator, tenantConfigurer );
		assertThatThrownBy( () -> searchMapping.clearAllAbortedEvents( (Object) "tenantX" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot target tenant 'tenantX'",
						"Currently configured tenant identifiers: [tenant1, tenant2, tenant3]."
				);
	}

	@ParameterizedTest(name = "entity = {0}")
	@MethodSource("params")
	void clearAllAbortedEvents(String indexName, Class<?> indexedEntity,
			BiConsumer<Session, Integer> entityCreator, Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		init( indexName, indexedEntity, entityCreator, tenantConfigurer );
		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();

		abortedEventsGenerator1.generateThreeAbortedEvents();
		abortedEventsGenerator2.generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();

		assertThat( searchMapping.clearAllAbortedEvents( TENANT_2_ID ) ).isEqualTo( 3 );

		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();
	}

	@ParameterizedTest(name = "entity = {0}")
	@MethodSource("params")
	void reprocessAbortedEvents(String indexName, Class<?> indexedEntity,
			BiConsumer<Session, Integer> entityCreator, Consumer<OrmSetupHelper.SetupContext> tenantConfigurer) {
		init( indexName, indexedEntity, entityCreator, tenantConfigurer );
		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();

		abortedEventsGenerator1.generateThreeAbortedEvents();
		List<Integer> generatedIds = abortedEventsGenerator2.generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();

		backendMock.expectWorks( indexName, TENANT_2_ID )
				.createAndExecuteFollowingWorks()
				.addOrUpdate( Integer.toString( generatedIds.get( 0 ) ), b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( Integer.toString( generatedIds.get( 1 ) ), b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( Integer.toString( generatedIds.get( 2 ) ), b -> b
						.field( "indexedField", "initialValue" )
				);
		assertThat( searchMapping.reprocessAbortedEvents( TENANT_2_ID ) ).isEqualTo( 3 );
		backendMock.verifyExpectationsMet();

		assertThat( searchMapping.countAbortedEvents( TENANT_1_ID ) ).isEqualTo( 3 );
		assertThat( searchMapping.countAbortedEvents( TENANT_2_ID ) ).isZero();
		assertThat( searchMapping.countAbortedEvents( TENANT_3_ID ) ).isZero();
	}
}
