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

import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.SimpleSessionFactoryBuilder;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

public class UnusedPropertiesIT {

	private static final String DEFAULT_BACKEND_PROPERTY_KEY = SearchOrmSettings.PREFIX + "index.default.backend";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	@Rule
	public ExpectedLog4jLog log = ExpectedLog4jLog.create();

	@Test
	public void checkDisabled_unusedProperty() {
		String unusedPropertyKey = "hibernate.search.index.default.foo";
		log.expectMessageMissing(
				"Some properties in the Hibernate Search configuration were not used"
		);
		log.expectMessage( "Configuration property tracking is disabled" );
		setup( builder -> {
			builder.setProperty( SearchOrmSettings.ENABLE_CONFIGURATION_PROPERTY_TRACKING, false );
			builder.setProperty( unusedPropertyKey, "bar" );
		} );
	}

	@Test
	public void checkEnabledByDefault_unusedProperty() {
		String unusedPropertyKey = "hibernate.search.index.default.foo";
		log.expectMessage(
				"Some properties in the Hibernate Search configuration were not used",
				"[" + unusedPropertyKey + "]"
		);
		log.expectMessageMissing( "Configuration property tracking is disabled" );
		// Also check that used properties are not reported as unused
		log.expectMessageMissing(
				"not used",
				DEFAULT_BACKEND_PROPERTY_KEY
		);

		setup( builder -> {
			builder.setProperty( unusedPropertyKey, "bar" );
		} );
	}

	@Test
	public void checkEnabledExplicitly_noUnusedProperty() {
		/*
		 * Check that the "enable configuration property tracking" property is considered used.
		 * This is a corner case worth testing, since the property may legitimately be accessed before
		 * we start tracking property usage.
 		 */
		log.expectMessageMissing( "Some properties in the Hibernate Search configuration were not used" );
		log.expectMessageMissing( "Configuration property tracking is disabled" );
		setup( builder -> {
			builder.setProperty( SearchOrmSettings.ENABLE_CONFIGURATION_PROPERTY_TRACKING, true );
		} );
	}

	private void setup(Consumer<SimpleSessionFactoryBuilder> configurationContributor) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "myTextField", String.class )
		);

		ormSetupHelper.withBackendMock( backendMock )
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
