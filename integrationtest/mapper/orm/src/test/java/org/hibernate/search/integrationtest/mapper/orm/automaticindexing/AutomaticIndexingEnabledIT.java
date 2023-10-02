/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test enabling/disabling automatic indexing.
 */
@TestForIssue(jiraKey = { "HSEARCH-4268", "HSEARCH-4616" })
class AutomaticIndexingEnabledIT {

	@SuppressWarnings("deprecation")
	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED ),
				Arguments.of( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED )
		);
	}

	private static final String DEPRECATED_STRATEGY_PROPERTY_MESSAGE = "Configuration property "
			+ "'hibernate.search.automatic_indexing.strategy' is deprecated;"
			+ " use 'hibernate.search.indexing.listeners.enabled' instead";

	private static final String DEPRECATED_AUTOMATIC_INDEXING_ENABLED_PROPERTY_MESSAGE = "Configuration property "
			+ "'hibernate.search.automatic_indexing.enabled' is deprecated;"
			+ " use 'hibernate.search.indexing.listeners.enabled' instead";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@SuppressWarnings("deprecation") // because of HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED
	private SessionFactory setup(Boolean enabled, String strategyName, String configurationSetting) {
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

	@ParameterizedTest(name = "Configuration Setting = {0}")
	@MethodSource("params")
	void enabled_default(String configurationSetting) {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).never();

		SessionFactory sessionFactory = setup( null, null, configurationSetting );

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

	@ParameterizedTest(name = "Configuration Setting = {0}")
	@MethodSource("params")
	void enabled_explicit(String configurationSetting) {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).never();

		SessionFactory sessionFactory = setup( true, null, configurationSetting );

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

	@ParameterizedTest(name = "Configuration Setting = {0}")
	@MethodSource("params")
	void disabled(String configurationSetting) {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).never();

		SessionFactory sessionFactory = setup( false, null, configurationSetting );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "Configuration Setting = {0}")
	@MethodSource("params")
	void legacy_strategy_none(String configurationSetting) {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).once();

		SessionFactory sessionFactory = setup( null, "none", configurationSetting );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "Configuration Setting = {0}")
	@MethodSource("params")
	void legacy_strategy_session(String configurationSetting) {
		logged.expectMessage( DEPRECATED_STRATEGY_PROPERTY_MESSAGE ).once();

		SessionFactory sessionFactory = setup( null, "session", configurationSetting );

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

	@SuppressWarnings("deprecation")
	@ParameterizedTest(name = "Configuration Setting = {0}")
	@MethodSource("params")
	void conflictingSettingsUsed(String configurationSetting) {
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
