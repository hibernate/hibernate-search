/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.Type;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test that the outbox polling strategy works fine regardless of how it is
 * referenced in the "coordination strategy" configuration property.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-4182")
public class OutboxPollingStrategyPropertyValueIT {

	@Parameterized.Parameters(name = "{0}")
	public static List<Object> params() {
		return Arrays.asList(
				HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME,
				"builtin:" + HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME,
				BeanReference.of(
						CoordinationStrategy.class,
						HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME,
						BeanRetrieval.BUILTIN
				)
		);
	}

	private static final OutboxEventFilter eventFilter = new OutboxEventFilter();

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@Parameterized.Parameter
	public Object strategyPropertyValue;

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		setupContext
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
				.withProperty( HibernateOrmMapperSettings.COORDINATION_STRATEGY, strategyPropertyValue )
				.withAnnotatedTypes( IndexedEntity.class );
	}

	@ReusableOrmSetupHolder.SetupParams
	public List<?> setupParams() {
		return Collections.singletonList( strategyPropertyValue );
	}

	@Before
	public void resetFilter() {
		eventFilter.reset();
	}

	@Test
	public void metamodel_userEntitiesAndOutboxEventAndAgent() {
		assertThat( setupHolder.sessionFactory().getJpaMetamodel().getEntities() )
				.<Class<?>>extracting( Type::getJavaType )
				.containsExactlyInAnyOrder( IndexedEntity.class, OutboxEvent.class, Agent.class );
	}

	@Test
	public void insert_createsOutboxEvent() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.persist( indexedPojo );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = eventFilter.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 1 );
		} );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		@Id
		private Integer id;
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

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
