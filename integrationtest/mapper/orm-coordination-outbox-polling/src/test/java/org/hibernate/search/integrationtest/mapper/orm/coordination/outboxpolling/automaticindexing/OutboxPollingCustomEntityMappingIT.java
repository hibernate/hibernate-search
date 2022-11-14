/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.FilteringOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.OutboxPollingAgentAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

public class OutboxPollingCustomEntityMappingIT {

	private static final String CUSTOM_SCHEMA = "CUSTOM_SCHEMA";
	private static final String ORIGINAL_OUTBOX_EVENT_TABLE_NAME = HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE;
	private static final String CUSTOM_OUTBOX_EVENT_TABLE_NAME = "CUSTOM_OUTBOX_EVENT";

	private static final String ORIGINAL_AGENT_TABLE_NAME = HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_AGENT_TABLE;
	private static final String CUSTOM_AGENT_TABLE_NAME = "CUSTOM_AGENT";
	private static final String ORIGINAL_AGENT_GENERATOR_NAME = ORIGINAL_AGENT_TABLE_NAME + "_GENERATOR";
	private static final String CUSTOM_AGENT_GENERATOR_NAME = CUSTOM_AGENT_TABLE_NAME + "_GENERATOR";

	private static final String VALID_OUTBOX_EVENT_MAPPING;
	private static final String VALID_AGENT_EVENT_MAPPING;

	private static final String[] SQL_KEYS;

	static {
		VALID_OUTBOX_EVENT_MAPPING = OutboxPollingOutboxEventAdditionalJaxbMappingProducer.ENTITY_DEFINITION
				.replace( ORIGINAL_OUTBOX_EVENT_TABLE_NAME, CUSTOM_OUTBOX_EVENT_TABLE_NAME );

		VALID_AGENT_EVENT_MAPPING = OutboxPollingAgentAdditionalJaxbMappingProducer.ENTITY_DEFINITION
				.replace( ORIGINAL_AGENT_TABLE_NAME, CUSTOM_AGENT_TABLE_NAME )
				.replace( ORIGINAL_AGENT_GENERATOR_NAME, CUSTOM_AGENT_GENERATOR_NAME );

		SQL_KEYS = new String[] {
				ORIGINAL_OUTBOX_EVENT_TABLE_NAME, CUSTOM_OUTBOX_EVENT_TABLE_NAME,
				ORIGINAL_AGENT_TABLE_NAME, CUSTOM_AGENT_TABLE_NAME,
				ORIGINAL_AGENT_GENERATOR_NAME, CUSTOM_AGENT_GENERATOR_NAME,
				CUSTOM_SCHEMA,
		};
	}

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;

	private final FilteringOutboxEventFinder outboxEventFinder = new FilteringOutboxEventFinder();

	@Test
	public void wrongOutboxEventMapping() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outboxevent.entity.mapping", "<entity-mappings><ciao></ciao></entity-mappings>" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContainingAll( "Unable to perform unmarshalling", "unexpected element" );
	}

	@Test
	public void wrongAgentMapping() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.agent.entity.mapping", "<entity-mappings><ciao></ciao></entity-mappings>" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContainingAll( "Unable to perform unmarshalling", "unexpected element" );
	}

	@Test
	public void validOutboxEventMapping() {
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

		if ( getDialect() instanceof MySQLDialect ) {
			// statements for sequences are not reported to the interceptor with this dialect
			return;
		}

		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_GENERATOR_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_GENERATOR_NAME ) ).isZero();
	}

	@Test
	public void validAgentMapping() {
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

		if ( getDialect() instanceof MySQLDialect ) {
			// statements for sequences are not reported to the interceptor with this dialect
			return;
		}

		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_GENERATOR_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_GENERATOR_NAME ) ).isPositive();
	}

	@Test
	public void conflictingAgentMappingConfiguration() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.agent.entity.mapping", VALID_AGENT_EVENT_MAPPING )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.table", "break_it_all" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Outbox polling agent configuration property conflict." );
	}

	@Test
	public void conflictingOutboxeventMappingConfiguration() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outboxevent.entity.mapping", VALID_OUTBOX_EVENT_MAPPING )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.table", "break_it_all" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Outbox event configuration property conflict." );
	}

	@Test
	public void validMappingWithCustomNames() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.generator", CUSTOM_AGENT_GENERATOR_NAME )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.table", CUSTOM_AGENT_TABLE_NAME )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.table", CUSTOM_OUTBOX_EVENT_TABLE_NAME )
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

		if ( getDialect() instanceof MySQLDialect ) {
			// statements for sequences are not reported to the interceptor with this dialect
			return;
		}

		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_GENERATOR_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_GENERATOR_NAME ) ).isPositive();
	}

	@Test
	public void validMappingWithCustomNamesAndSchema() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outbox_event_finder.provider", outboxEventFinder.provider() )
				// Allow ORM to create schema as we want to use non-default for this testcase:
				.withProperty( "javax.persistence.create-database-schemas", true )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.schema", CUSTOM_SCHEMA )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.generator", CUSTOM_AGENT_GENERATOR_NAME )
				.withProperty( "hibernate.search.coordination.entity.mapping.agent.table", CUSTOM_AGENT_TABLE_NAME )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.schema", CUSTOM_SCHEMA )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.table", CUSTOM_OUTBOX_EVENT_TABLE_NAME )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		assumeTrue( "This test only makes sense if the database supports schemas",
				getNameQualifierSupport().supportsSchemas() );
		assumeTrue( "This test only makes sense if the dialect supports creating schemas",
				getDialect().canCreateSchema() );

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
				List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( events )
						.hasSize( 1 )
						.extracting( OutboxEvent::getId )
						.extracting( UUID::version )
						.contains( 4 );
			} );
		} );
		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		outboxEventFinder.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_TABLE_NAME ) ).isPositive();

		assertThat( statementInspector.countByKey( CUSTOM_SCHEMA ) ).isPositive();

		if ( getDialect() instanceof MySQLDialect ) {
			// statements for sequences are not reported to the interceptor with this dialect
			return;
		}

		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_GENERATOR_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_GENERATOR_NAME ) ).isPositive();
	}

	@Test
	public void validMappingWithCustomUuidGenerator() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outbox_event_finder.provider", outboxEventFinder.provider() )
				// Allow ORM to create schema as we want to use non-default for this testcase:
				.withProperty( "javax.persistence.create-database-schemas", true )
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
				List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( events )
						.hasSize( 1 )
						.extracting( OutboxEvent::getId )
						.extracting( UUID::version )
						.contains( 1 );
			} );
		} );
		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		outboxEventFinder.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isPositive();
	}

	@Test
	public void validMappingWithCustomUuidDataType() {
		KeysStatementInspector statementInspector = new KeysStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outbox_event_finder.provider", outboxEventFinder.provider() )
				// Allow ORM to create schema as we want to use non-default for this testcase:
				.withProperty( "javax.persistence.create-database-schemas", true )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.preferred_uuid_jdbc_type", "uuid-char" )
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
				List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( events )
						.hasSize( 1 )
						.extracting( OutboxEvent::getId )
						.extracting( UUID::version )
						.contains( 4 );
			} );
		} );
		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		outboxEventFinder.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isPositive();
	}

	@Test
	public void validMappingWithCustomFailingUuidGenerator() {
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
