/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import java.util.function.Consumer;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

public class UnusedPropertiesIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void checkDisabled_unusedProperty() {
		String unusedPropertyKey = "hibernate.search.indexes.myIndex.foo";
		logged.expectMessage(
				"Some properties in the Hibernate Search configuration were not used"
		)
				.never();
		logged.expectMessage( "Configuration property tracking is disabled" )
				.once();
		setup( builder -> {
			builder.setProperty( EngineSettings.CONFIGURATION_PROPERTY_CHECKING_STRATEGY, "ignore" );
			builder.setProperty( unusedPropertyKey, "bar" );
		} );
	}

	@Test
	public void checkEnabledByDefault_unusedProperty() {
		String unusedPropertyKey = "hibernate.search.indexes.myIndex.foo";
		logged.expectMessage(
				"Some properties in the Hibernate Search configuration were not used",
				"[" + unusedPropertyKey + "]",
				"To disable this warning, set the property '"
						+ EngineSettings.CONFIGURATION_PROPERTY_CHECKING_STRATEGY + "' to 'ignore'"
		)
				.once();
		logged.expectMessage( "Configuration property tracking is disabled" )
				.never();
		// Also check that used properties are not reported as unused
		logged.expectMessage( "not used", HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE )
				.never();

		setup( builder -> {
			builder.setProperty( unusedPropertyKey, "bar" );
			builder.setProperty( HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE, 2 );
		} );
	}

	@Test
	public void checkEnabledExplicitly_noUnusedProperty() {
		/*
		 * Check that the "configuration property tracking strategy" property is considered used.
		 * This is a corner case worth testing, since the property may legitimately be accessed before
		 * we start tracking property usage.
 		 */
		logged.expectMessage( "Some properties in the Hibernate Search configuration were not used" )
				.never();
		logged.expectMessage( "Configuration property tracking is disabled" )
				.never();
		setup( builder -> {
			builder.setProperty( EngineSettings.CONFIGURATION_PROPERTY_CHECKING_STRATEGY, "warn" );
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
