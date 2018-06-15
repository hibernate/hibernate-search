/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.util.function.Consumer;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class OrmUnusedPropertiesIT {

	private static final String PREFIX = SearchOrmSettings.PREFIX;
	private static final String DEFAULT_BACKEND_PROPERTY_KEY = PREFIX + "index.default.backend";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public ExpectedLog4jLog log = ExpectedLog4jLog.create();

	private SessionFactory sessionFactory;

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void checkDisabled_unusedProperty() {
		String unusedPropertyKey = "hibernate.search.index.default.foo";
		log.expectMessageMissing(
				"Some properties in the Hibernate Search configuration were not used"
		);
		log.expectMessage( "Configuration property tracking is disabled" );
		setup( builder -> {
			builder.applySetting( SearchOrmSettings.ENABLE_CONFIGURATION_PROPERTY_TRACKING, false );
			builder.applySetting( unusedPropertyKey, "bar" );
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
			builder.applySetting( unusedPropertyKey, "bar" );
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
			builder.applySetting( SearchOrmSettings.ENABLE_CONFIGURATION_PROPERTY_TRACKING, true );
		} );
	}

	private void setup(Consumer<StandardServiceRegistryBuilder> propertyContributor) {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.applySetting( DEFAULT_BACKEND_PROPERTY_KEY, "stubBackend" );
		propertyContributor.accept( registryBuilder );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IndexedEntity.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();

		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "myTextField", String.class )
		);

		sessionFactory = sfb.build();
		backendMock.verifyExpectationsMet();
	}

	@Entity
	@Table(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Field(name = "myTextField")
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
