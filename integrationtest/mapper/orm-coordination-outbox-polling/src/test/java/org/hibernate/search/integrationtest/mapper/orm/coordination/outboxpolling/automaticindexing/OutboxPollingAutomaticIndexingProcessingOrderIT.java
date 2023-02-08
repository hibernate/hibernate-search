/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.ListAssert;

public class OutboxPollingAutomaticIndexingProcessingOrderIT {

	private static final String OUTBOX_EVENT_TABLE_NAME =
			HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Test
	public void invalid() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.event_processor.order", "someinvalidstring" )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.order': 'someinvalidstring'",
								"Invalid name for the outbox event processing order",
								"Valid names are: [auto, none, time, id]" ) );
	}

	@Test
	public void default_randomUuid() {
		StatementSpy spy = new StatementSpy();
		backendMock.expectAnySchema( IndexedEntity.NAME );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.session_factory.statement_inspector", spy )
				.setup( IndexedEntity.class );
		doSomeIndexing( sessionFactory );
		if ( sessionFactory.unwrap( SessionFactoryImplementor.class ).getJdbcServices()
				.getJdbcEnvironment().getDialect() instanceof SQLServerDialect ) {
			spy.assertStatements()
					.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
					.isNotEmpty()
					.noneSatisfy( st -> assertThat( st ).contains( "order" ) );
		}
		else {
			spy.assertStatements()
					.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
					.anySatisfy( st -> assertThat( st ).containsPattern( "order by \\w+.processAfter" ) );
		}
	}

	@Test
	public void default_timeUuid() {
		StatementSpy spy = new StatementSpy();
		backendMock.expectAnySchema( IndexedEntity.NAME );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.session_factory.statement_inspector", spy )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.uuid_gen_strategy", "time" )
				.setup( IndexedEntity.class );
		doSomeIndexing( sessionFactory );
		spy.assertStatements()
				.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
				.anySatisfy( st -> assertThat( st ).containsPattern( "order by \\w+.id" ) );
	}

	@Test
	public void auto_randomUuid() {
		StatementSpy spy = new StatementSpy();
		backendMock.expectAnySchema( IndexedEntity.NAME );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.session_factory.statement_inspector", spy )
				.withProperty( "hibernate.search.coordination.event_processor.order", "auto" )
				.setup( IndexedEntity.class );
		doSomeIndexing( sessionFactory );
		if ( sessionFactory.unwrap( SessionFactoryImplementor.class ).getJdbcServices()
				.getJdbcEnvironment().getDialect() instanceof SQLServerDialect ) {
			spy.assertStatements()
					.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
					.isNotEmpty()
					.noneSatisfy( st -> assertThat( st ).contains( "order" ) );
		}
		else {
			spy.assertStatements()
					.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
					.anySatisfy( st -> assertThat( st ).containsPattern( "order by \\w+.processAfter" ) );
		}
	}

	@Test
	public void auto_timeUuid() {
		StatementSpy spy = new StatementSpy();
		backendMock.expectAnySchema( IndexedEntity.NAME );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.session_factory.statement_inspector", spy )
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.uuid_gen_strategy", "time" )
				.withProperty( "hibernate.search.coordination.event_processor.order", "auto" )
				.setup( IndexedEntity.class );
		doSomeIndexing( sessionFactory );
		spy.assertStatements()
				.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
				.anySatisfy( st -> assertThat( st ).containsPattern( "order by \\w+.id" ) );
	}

	@Test
	public void none() {
		StatementSpy spy = new StatementSpy();
		backendMock.expectAnySchema( IndexedEntity.NAME );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.session_factory.statement_inspector", spy )
				.withProperty( "hibernate.search.coordination.event_processor.order", "none" )
				.setup( IndexedEntity.class );
		doSomeIndexing( sessionFactory );
		spy.assertStatements()
				.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
				.isNotEmpty()
				.noneSatisfy( st -> assertThat( st ).contains( "order" ) );
	}

	@Test
	public void time() {
		StatementSpy spy = new StatementSpy();
		backendMock.expectAnySchema( IndexedEntity.NAME );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.session_factory.statement_inspector", spy )
				.withProperty( "hibernate.search.coordination.event_processor.order", "time" )
				.setup( IndexedEntity.class );
		doSomeIndexing( sessionFactory );
		spy.assertStatements()
				.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
				.anySatisfy( st -> assertThat( st ).containsPattern( "order by \\w+.processAfter" ) );
	}

	@Test
	public void id() {
		StatementSpy spy = new StatementSpy();
		backendMock.expectAnySchema( IndexedEntity.NAME );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.session_factory.statement_inspector", spy )
				.withProperty( "hibernate.search.coordination.event_processor.order", "id" )
				.setup( IndexedEntity.class );
		doSomeIndexing( sessionFactory );
		spy.assertStatements()
				.filteredOn( st -> st.contains( "select" ) && st.contains( OUTBOX_EVENT_TABLE_NAME ) )
				.anySatisfy( st -> assertThat( st ).containsPattern( "order by \\w+.id" ) );
	}

	private void doSomeIndexing(SessionFactory sessionFactory) {
		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new IndexedEntity( 1, "someText" ) );
			backendMock.expectWorks( OutboxPollingAutomaticIndexingLifecycleIT.IndexedEntity.NAME )
					.add( "1", b -> b.field( "text", "someText" ) );

		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {
		static final String NAME = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@FullTextField
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	public static class StatementSpy implements StatementInspector {
		private final List<String> statements = Collections.synchronizedList( new ArrayList<>() );

		@Override
		public String inspect(String sql) {
			statements.add( sql );
			return sql;
		}

		public ListAssert<String> assertStatements() {
			return assertThat( statements );
		}
	}
}
