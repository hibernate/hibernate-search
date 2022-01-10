/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MappingException;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.OutboxPollingAgentAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Rule;
import org.junit.Test;

public class OutboxPollingCustomEntityMappingIT {

	private static final String ORIGINAL_OUTBOX_EVENT_TABLE_NAME = OutboxPollingOutboxEventAdditionalJaxbMappingProducer.TABLE_NAME;
	private static final String CUSTOM_OUTBOX_EVENT_TABLE_NAME = "CUSTOM_OUTBOX_EVENT";
	private static final String ORIGINAL_OUTBOX_EVENT_GENERATOR_NAME = ORIGINAL_OUTBOX_EVENT_TABLE_NAME + "_GENERATOR";
	private static final String CUSTOM_OUTBOX_EVENT_GENERATOR_NAME = CUSTOM_OUTBOX_EVENT_TABLE_NAME + "_GENERATOR";

	private static final String ORIGINAL_AGENT_TABLE_NAME = OutboxPollingAgentAdditionalJaxbMappingProducer.TABLE_NAME;
	private static final String CUSTOM_AGENT_TABLE_NAME = "CUSTOM_AGENT";
	private static final String ORIGINAL_AGENT_GENERATOR_NAME = ORIGINAL_AGENT_TABLE_NAME + "_GENERATOR";
	private static final String CUSTOM_AGENT_GENERATOR_NAME = CUSTOM_AGENT_TABLE_NAME + "_GENERATOR";

	private static final String VALID_OUTBOX_EVENT_MAPPING;
	private static final String VALID_AGENT_EVENT_MAPPING;

	private static final String[] SQL_KEYS;

	static {
		VALID_OUTBOX_EVENT_MAPPING = OutboxPollingOutboxEventAdditionalJaxbMappingProducer.ENTITY_DEFINITION
				.replace( ORIGINAL_OUTBOX_EVENT_TABLE_NAME, CUSTOM_OUTBOX_EVENT_TABLE_NAME )
				.replace( ORIGINAL_OUTBOX_EVENT_GENERATOR_NAME, CUSTOM_OUTBOX_EVENT_GENERATOR_NAME );

		VALID_AGENT_EVENT_MAPPING = OutboxPollingAgentAdditionalJaxbMappingProducer.ENTITY_DEFINITION
				.replace( ORIGINAL_AGENT_TABLE_NAME, CUSTOM_AGENT_TABLE_NAME )
				.replace( ORIGINAL_AGENT_GENERATOR_NAME, CUSTOM_AGENT_GENERATOR_NAME );

		SQL_KEYS = new String[] {
				ORIGINAL_OUTBOX_EVENT_TABLE_NAME, CUSTOM_OUTBOX_EVENT_TABLE_NAME,
				ORIGINAL_OUTBOX_EVENT_GENERATOR_NAME, CUSTOM_OUTBOX_EVENT_GENERATOR_NAME,
				ORIGINAL_AGENT_TABLE_NAME, CUSTOM_AGENT_TABLE_NAME,
				ORIGINAL_AGENT_GENERATOR_NAME, CUSTOM_AGENT_GENERATOR_NAME
		};
	}

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;

	@Test
	public void wrongOutboxEventMapping() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outboxevent.entity.mapping", "<ciao></ciao>" )
				.setup( IndexedEntity.class )
		)
				.isInstanceOf( MappingException.class )
				.hasMessageContainingAll( "Unable to perform unmarshalling", "unexpected element" );
	}

	@Test
	public void wrongAgentMapping() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.agent.entity.mapping", "<ciao></ciao>" )
				.setup( IndexedEntity.class )
		)
				.isInstanceOf( MappingException.class )
				.hasMessageContainingAll( "Unable to perform unmarshalling", "unexpected element" );
	}

	@Test
	public void validOutboxEventMapping() {
		MostRecentStatementInspector statementInspector = new MostRecentStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outboxevent.entity.mapping", VALID_OUTBOX_EVENT_MAPPING )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		backendMock.expectWorks( IndexedEntity.INDEX )
				.add( "1", f -> f.field( "indexedField", "value for the field" ) );

		int id = 1;
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_GENERATOR_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_GENERATOR_NAME ) ).isPositive();

		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_GENERATOR_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_GENERATOR_NAME ) ).isZero();
	}

	@Test
	public void validAgentMapping() {
		MostRecentStatementInspector statementInspector = new MostRecentStatementInspector();

		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.agent.entity.mapping", VALID_AGENT_EVENT_MAPPING )
				.withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		backendMock.expectWorks( IndexedEntity.INDEX )
				.add( "1", f -> f.field( "indexedField", "value for the field" ) );

		int id = 1;
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		backendMock.verifyExpectationsMet();

		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( ORIGINAL_OUTBOX_EVENT_GENERATOR_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( CUSTOM_OUTBOX_EVENT_GENERATOR_NAME ) ).isZero();

		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_TABLE_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_TABLE_NAME ) ).isPositive();
		assertThat( statementInspector.countByKey( ORIGINAL_AGENT_GENERATOR_NAME ) ).isZero();
		assertThat( statementInspector.countByKey( CUSTOM_AGENT_GENERATOR_NAME ) ).isPositive();
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

	public static class MostRecentStatementInspector implements StatementInspector {

		private Map<String, List<String>> sqlByKey = new HashMap<>();

		public MostRecentStatementInspector() {
			for ( String key : SQL_KEYS ) {
				sqlByKey.put( key, new ArrayList<>() );
			}
		}

		@Override
		public String inspect(String sql) {
			for ( String key : SQL_KEYS ) {
				if ( sql.contains( key ) ) {
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
