/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test enabling/disabling automatic indexing.
 */
@TestForIssue(jiraKey = { "HSEARCH-4268", "HSEARCH-4616" })
@RunWith(Parameterized.class)
public class AutomaticIndexingEnabledIT {

	@Parameterized.Parameters(name = "Configuration Setting = {0}")
	@SuppressWarnings("deprecation")
	public static List<Object[]> data() {
		return Arrays.asList( new Object[][] {
				{ HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED },
				{ HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED }
		} );
	}

	private static final String DEPRECATED_STRATEGY_PROPERTY_MESSAGE = "Configuration property "
			+ "'hibernate.search.automatic_indexing.strategy' is deprecated;"
			+ " use 'hibernate.search.indexing.listeners.enabled' instead";

	private static final String DEPRECATED_AUTOMATIC_INDEXING_ENABLED_PROPERTY_MESSAGE = "Configuration property "
			+ "'hibernate.search.automatic_indexing.enabled' is deprecated;"
			+ " use 'hibernate.search.indexing.listeners.enabled' instead";

	@Parameterized.Parameter
	public String configurationSetting;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@SuppressWarnings("deprecation") // because of HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED
	private SessionFactory setup(Boolean enabled, String strategyName) {
		if ( enabled != null && HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED.equals( configurationSetting ) ) {
			logged.expectMessage( DEPRECATED_AUTOMATIC_INDEXING_ENABLED_PROPERTY_MESSAGE ).once();
		}

		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
		);

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( configurationSetting, enabled )
				.withProperty( "hibernate.search.automatic_indexing.strategy", strategyName )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
		return sessionFactory;
	}

	@Test
	public void enabled_default() {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).never();

		SessionFactory sessionFactory = setup( null, null );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", entity1.getText() )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void enabled_explicit() {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).never();

		SessionFactory sessionFactory = setup( true, null );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", entity1.getText() )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void disabled() {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).never();

		SessionFactory sessionFactory = setup( false, null );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void legacy_strategy_none() {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).once();

		SessionFactory sessionFactory = setup( null, "none" );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void legacy_strategy_session() {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).once();

		SessionFactory sessionFactory = setup( null, "session" );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", entity1.getText() )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void conflictingSettingsUsed() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
		);

		assertThatThrownBy( () -> ormSetupHelper.start()
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, true )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, true )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Both 'hibernate.search.automatic_indexing.enabled' and 'hibernate.search.indexing.listeners.enabled' are configured."
								+
								" Use only 'hibernate.search.indexing.listeners.enabled' to enable indexing listeners." );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "Indexed";

		@Id
		private Integer id;

		@Basic
		@GenericField
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

}
