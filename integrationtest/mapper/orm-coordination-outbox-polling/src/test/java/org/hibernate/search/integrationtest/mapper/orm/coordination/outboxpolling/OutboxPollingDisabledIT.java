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
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Check that even with the outbox-polling JAR in the classpath,
 * disabling Hibernate Search or picking a different coordination strategy
 * will disable the addition of OutboxEvent entities to the Hibernate ORM model.
 */
public class OutboxPollingDisabledIT {

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				// We must not add OutboxEvent to the model if the coordination strategy is "none"
				Arguments.of( true, "none" ),
				// We must not add OutboxEvent to the model if Hibernate Search is disabled
				Arguments.of( false, HibernateOrmMapperOutboxPollingSettings.COORDINATION_STRATEGY_NAME )
		);
	}

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.createGlobal();

	@RegisterExtension
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.defaults() )
			.delayedInitialization( true );

	@RegisterExtension
	public Extension setupHolderMethodRule = setupHolder.methodExtension();

	public boolean hibernateSearchEnabled;

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

	void init(boolean hibernateSearchEnabled, String coordinationStrategyName) {
		this.hibernateSearchEnabled = hibernateSearchEnabled;
		this.coordinationStrategyName = coordinationStrategyName;

		setupHolder.initialize();
	}

	@ParameterizedTest(name = "enabled = {0}, strategy = {1}")
	@MethodSource("params")
	void metamodel_onlyUserEntities(boolean hibernateSearchEnabled, String coordinationStrategyName) {
		init( hibernateSearchEnabled, coordinationStrategyName );
		assertThat( setupHolder.sessionFactory().getMetamodel().getEntities() )
				.extracting( EntityType::getName )
				.containsOnly( IndexedEntity.NAME );
	}

	@ParameterizedTest(name = "enabled = {0}, strategy = {1}")
	@MethodSource("params")
	void queryOutboxEvent_exception(boolean hibernateSearchEnabled, String coordinationStrategyName) {
		init( hibernateSearchEnabled, coordinationStrategyName );
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
