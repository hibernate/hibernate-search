/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.outboxpolling.avro.impl.EventPayloadSerializationUtils;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.OutboxPollingAgentAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OutboxPollingCustomEntityMappingIT {

	private static final String POSTGRESQL_DIALECT = "org.hibernate.dialect.PostgreSQLDialect";
	private static final String MSSQL_DIALECT = "org.hibernate.dialect.SQLServerDialect";

	private static final String CUSTOM_SCHEMA = "CUSTOM_SCHEMA";
	private static final String ORIGINAL_OUTBOX_EVENT_TABLE_NAME =
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE;
	private static final String CUSTOM_OUTBOX_EVENT_TABLE_NAME = "CUSTOM_OUTBOX_EVENT";

	private static final String ORIGINAL_AGENT_TABLE_NAME =
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_TABLE;
	private static final String CUSTOM_AGENT_TABLE_NAME = "CUSTOM_AGENT";
	private static final String VALID_OUTBOX_EVENT_MAPPING;
	private static final String VALID_AGENT_EVENT_MAPPING;

	private static final String[] SQL_KEYS;

	static {
		VALID_OUTBOX_EVENT_MAPPING = OutboxPollingOutboxEventAdditionalJaxbMappingProducer.ENTITY_DEFINITION
				.replace( ORIGINAL_OUTBOX_EVENT_TABLE_NAME, CUSTOM_OUTBOX_EVENT_TABLE_NAME );

		VALID_AGENT_EVENT_MAPPING = OutboxPollingAgentAdditionalJaxbMappingProducer.ENTITY_DEFINITION
				.replace( ORIGINAL_AGENT_TABLE_NAME, CUSTOM_AGENT_TABLE_NAME );

		SQL_KEYS = new String[] {
				ORIGINAL_OUTBOX_EVENT_TABLE_NAME,
				CUSTOM_OUTBOX_EVENT_TABLE_NAME,
				ORIGINAL_AGENT_TABLE_NAME,
				CUSTOM_AGENT_TABLE_NAME,
				CUSTOM_SCHEMA,
		};
	}

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	private final OutboxEventFilter eventFilter = new OutboxEventFilter();

	@Test
	void wrongOutboxEventMapping() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outboxevent.entity.mapping",
						"<entity-mappings><ciao></ciao></entity-mappings>" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to process provided entity mappings", "unexpected element" );
	}

	@Test
	void wrongAgentMapping() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.agent.entity.mapping",
						"<entity-mappings><ciao></ciao></entity-mappings>" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to process provided entity mappings", "unexpected element" );
	}

	@Test
	void validOutboxEventMapping() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outboxevent.entity.mapping", VALID_OUTBOX_EVENT_MAPPING )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", f -> f.field( "indexedField", "value for the field" ) );
		} );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_TABLE_NAME ) ).isZero();
	}

	@Test
	void validAgentMapping() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.agent.entity.mapping", VALID_AGENT_EVENT_MAPPING )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", f -> f.field( "indexedField", "value for the field" ) );
		} );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_TABLE_NAME ) ).isPositive();
	}

	@Test
	void conflictingAgentMappingConfiguration() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.agent.entity.mapping", VALID_AGENT_EVENT_MAPPING )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.table", "break_it_all" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Outbox polling agent configuration property conflict." );
	}

	@Test
	void conflictingOutboxeventMappingConfiguration() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outboxevent.entity.mapping", VALID_OUTBOX_EVENT_MAPPING )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.table", "break_it_all" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Outbox event configuration property conflict." );
	}

	@Test
	void validMappingWithCustomNames() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.table", CUSTOM_AGENT_TABLE_NAME )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.table",
						CUSTOM_OUTBOX_EVENT_TABLE_NAME )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", f -> f.field( "indexedField", "value for the field" ) );
		} );
		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_TABLE_NAME ) ).isPositive();
	}

	@Test
	void validMappingWithCustomNamesAndSchema() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
				// Allow ORM to create schema as we want to use non-default for this testcase:
				.withProperty( "jakarta.persistence.create-database-schemas", true )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.schema", CUSTOM_SCHEMA )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.table", CUSTOM_AGENT_TABLE_NAME )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.schema", CUSTOM_SCHEMA )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.table",
						CUSTOM_OUTBOX_EVENT_TABLE_NAME )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		assumeTrue(
				getNameQualifierSupport().supportsSchemas(),
				"This test only makes sense if the database supports schemas"
		);
		assumeTrue(
				getDialect().canCreateSchema(),
				"This test only makes sense if the dialect supports creating schemas"
		);

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", f -> f.field( "indexedField", "value for the field" ) );
		} );
		await().untilAsserted( () -> {
			with( sessionFactory ).runInTransaction( session -> {
				// check that correct UUIDs are generated by asserting the version:
				assertEventUUIDVersion( session, 4 );
				assertAgentUUIDVersion( session, 4 );
			} );
		} );
		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		eventFilter.showAllEventsUpToNow( sessionFactory );
		eventFilter.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_TABLE_NAME ) ).isPositive();

		assertThat( statementInspector.countByKey( CUSTOM_SCHEMA ) ).isPositive();
	}

	@Test
	void validMappingWithCustomUuidGenerator() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
				// Allow ORM to create schema as we want to use non-default for this testcase:
				.withProperty( "jakarta.persistence.create-database-schemas", true )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.uuid_gen_strategy", "time" )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.uuid_gen_strategy", "time" )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", f -> f.field( "indexedField", "value for the field" ) );
		} );

		await().untilAsserted( () -> {
			with( sessionFactory ).runInTransaction( session -> {
				// check that correct UUIDs are generated by asserting the version:
				assertEventUUIDVersion( session, 1 );
				assertAgentUUIDVersion( session, 1 );
			} );
		} );
		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		eventFilter.showAllEventsUpToNow( sessionFactory );
		eventFilter.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isPositive();
	}

	@Test
	void validMappingWithCustomUuidDataType() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
				// Allow ORM to create schema as we want to use non-default for this testcase:
				.withProperty( "jakarta.persistence.create-database-schemas", true )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.uuid_type", "CHAR" )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.uuid_type", "CHAR" )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", f -> f.field( "indexedField", "value for the field" ) );
		} );

		await().untilAsserted( () -> {
			with( sessionFactory ).runInTransaction( session -> {
				// check that correct UUIDs are generated by asserting the version:
				assertEventUUIDVersion( session, 4 );
				assertAgentUUIDVersion( session, 4 );
			} );
		} );
		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		eventFilter.showAllEventsUpToNow( sessionFactory );
		eventFilter.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isPositive();
	}

	@Test
	void validMappingWithCustomFailingUuidGenerator() {
		assertThatThrownBy(
				() -> ormSetupHelper.start()
						.withProperty(
								"hibernate.search.coordination.entity.mapping.outboxevent.uuid_gen_strategy",
								"something-incompatible"
						)
						.setup( IndexedEntity.class )
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid value for configuration property 'hibernate.search.coordination.entity.mapping.outboxevent.uuid_gen_strategy'",
						"something-incompatible",
						"Valid names are: [auto, random, time]"
				);
	}

	@Test
	void defaultUuidDataType() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter )
				)
				// Allow ORM to create schema as we want to use non-default for this testcase:
				.withProperty( "jakarta.persistence.create-database-schemas", true )
				.withProperty( "hibernate.show_sql", true )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		JdbcType agent = idJdbcType( sessionFactory, Agent.class );
		JdbcType event = idJdbcType( sessionFactory, OutboxEvent.class );

		String dialect = DatabaseContainer.configuration().dialect();

		switch ( dialect ) {
			case POSTGRESQL_DIALECT:
			case MSSQL_DIALECT:
				assertThat( agent.getDdlTypeCode() ).isEqualTo( SqlTypes.UUID );
				assertThat( event.getDdlTypeCode() ).isEqualTo( SqlTypes.UUID );
				break;
			default:
				assertThat( agent.getDdlTypeCode() ).isIn( SqlTypes.UUID, SqlTypes.BINARY, SqlTypes.CHAR );
				assertThat( event.getDdlTypeCode() ).isIn( SqlTypes.UUID, SqlTypes.BINARY, SqlTypes.CHAR );
				break;
		}
	}

	private static JdbcType idJdbcType(SessionFactory sessionFactory, Class<?> entity) {
		SimpleDomainType<?> idType = sessionFactory.unwrap( SessionFactoryImplementor.class ).getJpaMetamodel().entity( entity )
				.getIdType();

		return ( (JdbcMapping) idType ).getJdbcType();
	}

	private void assertEventUUIDVersion(Session session, int expectedVersion) {
		List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
		assertThat( events )
				.hasSize( 1 )
				.extracting( OutboxEvent::getId )
				.extracting( UUID::version )
				.containsOnly( expectedVersion );
	}

	private void assertAgentUUIDVersion(Session session, int expectedVersion) {
		assertThat(
				session.createQuery(
						"select a from " + OutboxPollingAgentAdditionalJaxbMappingProducer.ENTITY_NAME + " a ",
						Agent.class
				)
						.getResultList()
		).hasSizeGreaterThan( 0 )
				.extracting( Agent::getId )
				.extracting( UUID::version )
				.containsOnly( expectedVersion );
	}

	private void assertEventPayload(Session session) {
		List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
		assertThat( events ).hasSize( 1 );
		byte[] payload = events.get( 0 ).getPayload();
		assertThat( payload ).isNotEmpty();
		// check that we can deserialize whatever was saved:
		PojoIndexingQueueEventPayload deserialized = EventPayloadSerializationUtils.deserialize( payload );
		assertThat( deserialized.routes.currentRoute().routingKey() ).isNull();
	}

	private void assertAgentPayload(Session session) {
		List<Agent> agents = session.createQuery(
				"select a from " + OutboxPollingAgentAdditionalJaxbMappingProducer.ENTITY_NAME + " a ",
				Agent.class
		)
				.getResultList();
		assertThat( agents ).hasSize( 1 );
		// agent payload is currently null
		assertThat( agents.get( 0 ).getPayload() ).isNull();
	}

	private Dialect getDialect() {
		return sessionFactory.unwrap( SessionFactoryImplementor.class ).getJdbcServices()
				.getJdbcEnvironment().getDialect();
	}

	private NameQualifierSupport getNameQualifierSupport() {
		return sessionFactory.unwrap( SessionFactoryImplementor.class ).getJdbcServices()
				.getJdbcEnvironment().getNameQualifierSupport();
	}

	@Entity(name = IndexedEntity.INDEX)
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {
		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

	public static class KeysStatementInspector implements StatementInspector {

		private Map<String, List<String>> sqlByKey = new HashMap<>();

		public KeysStatementInspector() {
			for ( String key : SQL_KEYS ) {
				sqlByKey.put( key, new ArrayList<>() );
			}
		}

		@Override
		public String inspect(String sql) {
			for ( String key : SQL_KEYS ) {
				if ( Arrays.stream( sql.split( "[^A-Za-z0-9_-]" ) ).anyMatch( token -> key.equals( token ) ) ) {
					sqlByKey.get( key ).add( sql );
				}
			}
			return sql;
		}

		public int countByKey(String key) {
			return sqlByKey.get( key ).size();
		}
	}
}
