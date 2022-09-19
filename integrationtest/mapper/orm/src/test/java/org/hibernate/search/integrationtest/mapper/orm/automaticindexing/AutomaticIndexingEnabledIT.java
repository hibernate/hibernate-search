/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test enabling/disabling automatic indexing.
 */
@TestForIssue(jiraKey = "HSEARCH-4268")
public class AutomaticIndexingEnabledIT {

	private static final String DEPRECATED_STRATEGY_PROPERTY_MESSAGE = "Configuration property "
			+ "'hibernate.search.automatic_indexing.strategy' is deprecated;"
			+ " use 'hibernate.search.automatic_indexing.enabled' instead";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private SessionFactory setup(Boolean enabled, String strategyName) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
		);

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.automatic_indexing.enabled", enabled )
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
