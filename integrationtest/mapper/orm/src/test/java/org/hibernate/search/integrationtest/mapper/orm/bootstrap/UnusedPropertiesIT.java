/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

class UnusedPropertiesIT {
	private static final String KEY_UNUSED = "hibernate.search.indexes.myIndex.foo";
	private static final String KEY_UNUSED_BUT_EMPTY_VALUE = "hibernate.search.indexes.myIndex.emptyValue";
	private static final String KEY_UNUSED_BUT_BLANK_VALUE = "hibernate.search.indexes.myIndex.blankValue";
	private static final String KEY_UNUSED_BUT_NULL_VALUE = "hibernate.search.indexes.myIndex.nullValue";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	void checkDisabled_unusedProperty() {
		logged.expectMessage( "some properties in the given configuration are not used" )
				.never();
		logged.expectEvent( Level.INFO, "Configuration property tracking is disabled" )
				.once();
		setup( builder -> {
			builder.setProperty( EngineSettings.CONFIGURATION_PROPERTY_CHECKING_STRATEGY, "ignore" );
			builder.setProperty( KEY_UNUSED, "bar" );
			// These properties should be ignored
			builder.setProperty( KEY_UNUSED_BUT_EMPTY_VALUE, "" );
			builder.setProperty( KEY_UNUSED_BUT_BLANK_VALUE, "   " );
			builder.setProperty( KEY_UNUSED_BUT_NULL_VALUE, null );
		} );
	}

	@Test
	void checkEnabledByDefault_unusedProperty() {
		logged.expectEvent( Level.WARN,
				"Invalid configuration passed to Hibernate Search",
				"some properties in the given configuration are not used",
				"[" + KEY_UNUSED + "]",
				"To disable this warning, set the property '"
						+ EngineSettings.CONFIGURATION_PROPERTY_CHECKING_STRATEGY + "' to 'ignore'" )
				.once();
		logged.expectMessage( "Configuration property tracking is disabled" )
				.never();
		// Also check that used properties are not reported as unused
		logged.expectMessage( "not used", HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE )
				.never();

		setup( builder -> {
			builder.setProperty( KEY_UNUSED, "bar" );
			builder.setProperty( HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE, 2 );
			// These properties should be ignored
			builder.setProperty( KEY_UNUSED_BUT_EMPTY_VALUE, "" );
			builder.setProperty( KEY_UNUSED_BUT_BLANK_VALUE, "   " );
			builder.setProperty( KEY_UNUSED_BUT_NULL_VALUE, null );
		} );
	}

	@Test
	void checkEnabledExplicitly_noUnusedProperty() {
		/*
		 * Check that the "configuration property tracking strategy" property is considered used.
		 * This is a corner case worth testing, since the property may legitimately be accessed before
		 * we start tracking property usage.
		 */
		logged.expectMessage( "some properties in the given configuration are not used" )
				.never();
		logged.expectMessage( "Configuration property tracking is disabled" )
				.never();
		setup( builder -> {
			builder.setProperty( EngineSettings.CONFIGURATION_PROPERTY_CHECKING_STRATEGY, "warn" );
			// These properties should be ignored
			builder.setProperty( KEY_UNUSED_BUT_EMPTY_VALUE, "" );
			builder.setProperty( KEY_UNUSED_BUT_BLANK_VALUE, "   " );
			builder.setProperty( KEY_UNUSED_BUT_NULL_VALUE, null );
		} );
	}

	private void setup(Consumer<SimpleSessionFactoryBuilder> configurationContributor) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "myTextField", String.class )
		);

		ormSetupHelper.start()
				.withConfiguration( configurationContributor )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Entity
	@Table(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@GenericField(name = "myTextField")
		private String text;

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
