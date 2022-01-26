/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.EntityType;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Check that even with the outbox-polling JAR in the classpath,
 * disabling Hibernate Search or picking a different coordination strategy
 * will disable the addition of OutboxEvent entities to the Hibernate ORM model.
 */
@RunWith(Parameterized.class)
public class OutboxPollingDisabledIT {

	@Parameterized.Parameters(name = "enabled = {0}, strategy = {1}")
	public static Object[][] params() {
		return new Object[][] {
				// We must not add OutboxEvent to the model if the coordination strategy is "none"
				{ true, "none" },
				// We must not add OutboxEvent to the model if Hibernate Search is disabled
				{ false, HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME },
		};
	}

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.defaults() );
	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@Parameterized.Parameter
	public boolean hibernateSearchEnabled;

	@Parameterized.Parameter(1)
	public String coordinationStrategyName;

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		if ( hibernateSearchEnabled ) {
			backendMock.expectSchema( IndexedEntity.NAME, b -> b.field( "indexedField", String.class ) );
		}
		setupContext.withAnnotatedTypes( IndexedEntity.class )
				.withProperty( HibernateOrmMapperSettings.ENABLED, hibernateSearchEnabled )
				.withProperty( HibernateOrmMapperSettings.COORDINATION_STRATEGY, coordinationStrategyName );
	}

	@ReusableOrmSetupHolder.SetupParams
	public List<?> setupParams() {
		return Arrays.asList( hibernateSearchEnabled, coordinationStrategyName );
	}

	@Test
	public void metamodel_onlyUserEntities() {
		assertThat( setupHolder.sessionFactory().getJpaMetamodel().getEntities() )
				.extracting( EntityType::getName )
				.containsOnly( IndexedEntity.NAME );
	}

	@Test
	public void queryOutboxEvent_exception() {
		setupHolder.runInTransaction( s -> assertThatThrownBy( () -> {
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
