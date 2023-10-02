/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.EntityType;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Check that even with the outbox-polling JAR in the classpath,
 * disabling Hibernate Search or picking a different coordination strategy
 * will disable the addition of OutboxEvent entities to the Hibernate ORM model.
 */
@ParameterizedPerClass
public class OutboxPollingDisabledIT {

	private SessionFactory sessionFactory;

	public static List<? extends Arguments> params() {
		return List.of(
				// We must not add OutboxEvent to the model if the coordination strategy is "none"
				Arguments.of( true, "none" ),
				// We must not add OutboxEvent to the model if Hibernate Search is disabled
				Arguments.of( false, HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME )
		);
	}

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.defaults() )
					.withBackendMock( backendMock );


	@ParameterizedSetup
	@MethodSource("params")
	void setup(boolean hibernateSearchEnabled, String coordinationStrategyName) {
		if ( hibernateSearchEnabled ) {
			backendMock.expectSchema( IndexedEntity.NAME, b -> b.field( "indexedField", String.class ) );
		}
		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( IndexedEntity.class )
				.withProperty( HibernateOrmMapperSettings.ENABLED, hibernateSearchEnabled )
				.withProperty( HibernateOrmMapperSettings.COORDINATION_STRATEGY, coordinationStrategyName )
				.setup();
	}

	@Test
	void metamodel_onlyUserEntities() {
		assertThat( sessionFactory.unwrap( SessionFactoryImplementor.class ).getJpaMetamodel().getEntities() )
				.extracting( EntityType::getName )
				.containsOnly( IndexedEntity.NAME );
	}

	@Test
	void queryOutboxEvent_exception() {
		with( sessionFactory ).runInTransaction( s -> assertThatThrownBy( () -> {
			s.getCriteriaBuilder().createQuery( OutboxEvent.class ).from( OutboxEvent.class );
		} )
				.isInstanceOf( IllegalArgumentException.class ) );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

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
