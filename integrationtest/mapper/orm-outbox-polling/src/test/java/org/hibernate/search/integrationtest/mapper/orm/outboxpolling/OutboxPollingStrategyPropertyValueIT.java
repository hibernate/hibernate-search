/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.Type;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetupBeforeTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that the outbox polling strategy works fine regardless of how it is
 * referenced in the "coordination strategy" configuration property.
 */
@ParameterizedPerClass
@TestForIssue(jiraKey = "HSEARCH-4182")
public class OutboxPollingStrategyPropertyValueIT {

	private SessionFactory sessionFactory;

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME ),
				Arguments.of( "builtin:" + HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME ),
				Arguments.of( BeanReference.of(
						CoordinationStrategy.class,
						HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME,
						BeanRetrieval.BUILTIN
				) )
		);
	}

	private static final OutboxEventFilter eventFilter = new OutboxEventFilter();

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	@ParameterizedSetup
	@MethodSource("params")
	void setup(Object strategyPropertyValue) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter )
				)
				.withProperty( HibernateOrmMapperSettings.COORDINATION_STRATEGY, strategyPropertyValue )
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
	}

	@ParameterizedSetupBeforeTest
	void resetFilter() {
		eventFilter.reset();
	}

	@Test
	void metamodel_userEntitiesAndOutboxEventAndAgent() {
		assertThat( sessionFactory.unwrap( SessionFactoryImplementor.class ).getJpaMetamodel().getEntities() )
				.<Class<?>>extracting( Type::getJavaType )
				.containsExactlyInAnyOrder( IndexedEntity.class, OutboxEvent.class, Agent.class );
	}

	@Test
	void insert_createsOutboxEvent() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.persist( indexedPojo );
		} );

		with( sessionFactory ).runInTransaction( session -> {
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
