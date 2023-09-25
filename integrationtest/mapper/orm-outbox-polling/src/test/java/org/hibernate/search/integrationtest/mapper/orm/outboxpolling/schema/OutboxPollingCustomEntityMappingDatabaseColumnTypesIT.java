/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.sql.Types;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

public class OutboxPollingCustomEntityMappingDatabaseColumnTypesIT {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;

	@Test
	public void test() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.show_sql", true )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();


		MappingMetamodel domainModel = sessionFactory
				.unwrap( SessionFactoryImplementor.class ).getRuntimeMetamodels().getMappingMetamodel();
		EntityPersister outboxEvent = domainModel.findEntityDescriptor( OutboxEvent.class );
		outboxEvent.getEntityMappingType().getAttributeMappings().forEach( attr -> {
			if ( attr.getAttributeName().equals( "payload" ) ) {
				Dialect dialect = getDialect();
				checkPayloadAttribute( attr, dialect );
			}
		} );

		EntityPersister agent = domainModel.findEntityDescriptor( Agent.class );
		agent.getEntityMappingType().getAttributeMappings().forEach( attr -> {
			if ( attr.getAttributeName().equals( "payload" ) ) {
				Dialect dialect = getDialect();
				checkPayloadAttribute( attr, dialect );
			}
		} );
	}

	private static void checkPayloadAttribute(AttributeMapping attr, Dialect dialect) {
		if ( dialect instanceof OracleDialect ) {
			assertThat( attr.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ).isEqualTo( Types.BLOB );
		}
		else if ( dialect instanceof PostgreSQLDialect ) {
			assertThat( attr.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ).isEqualTo( Types.VARBINARY );
		}
		else if ( ( dialect instanceof MariaDBDialect ) || dialect instanceof MySQLDialect ) {
			assertThat( attr.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ).isEqualTo( Types.VARBINARY );
		}
		else if ( dialect instanceof CockroachDialect ) {
			assertThat( attr.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ).isEqualTo( Types.VARBINARY );
		}
		else if ( dialect instanceof SQLServerDialect ) {
			assertThat( attr.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ).isEqualTo( Types.BLOB );
		}
		else if ( dialect instanceof DB2Dialect ) {
			assertThat( attr.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ).isEqualTo( Types.BLOB );
		}
		else if ( dialect instanceof H2Dialect ) {
			assertThat( attr.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ).isEqualTo( Types.BLOB );
		}
		else {
			fail( "Unknown dialect " + dialect );
		}
	}

	private Dialect getDialect() {
		return sessionFactory.unwrap( SessionFactoryImplementor.class ).getJdbcServices()
				.getJdbcEnvironment().getDialect();
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
}
