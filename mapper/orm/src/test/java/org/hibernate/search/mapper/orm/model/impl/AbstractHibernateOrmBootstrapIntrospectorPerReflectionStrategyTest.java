/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.util.common.impl.Closer;

import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractHibernateOrmBootstrapIntrospectorPerReflectionStrategyTest {

	@Parameterized.Parameters(name = "Reflection strategy = {0}")
	public static List<Object[]> data() {
		return Arrays.asList( new Object[][] {
				{ null },
				{ "method-handle" },
				{ "java-lang-reflect" }
		} );
	}

	private final List<AutoCloseable> toClose = new ArrayList<>();

	@Parameterized.Parameter
	public String reflectionStrategyName = "java-lang-reflect";

	@After
	public void cleanup() throws Exception {
		try ( Closer<Exception> closer = new Closer<>() ) {
			closer.pushAll( AutoCloseable::close, toClose );
		}
	}

	final HibernateOrmBootstrapIntrospector createIntrospector(Class<?> ... entityClasses) {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		// Some properties that are not relevant to our test, but necessary to create the Metadata
		registryBuilder.applySetting( AvailableSettings.DIALECT, H2Dialect.class );
		StandardServiceRegistry serviceRegistry = registryBuilder.build();
		toClose.add( serviceRegistry );

		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		for ( Class<?> entityClass : entityClasses ) {
			metadataSources.addAnnotatedClass( entityClass );
		}
		Metadata metadata = metadataSources.buildMetadata();

		MetadataImplementor metadataImplementor = (MetadataImplementor) metadata;
		ReflectionManager reflectionManager = metadataImplementor.getTypeConfiguration()
				.getMetadataBuildingContext().getBootstrapContext().getReflectionManager();

		Map<String, Object> properties = new HashMap<>();
		if ( reflectionStrategyName != null ) {
			properties.put(
					HibernateOrmMapperSpiSettings.Radicals.REFLECTION_STRATEGY,
					reflectionStrategyName
			);
		}

		return HibernateOrmBootstrapIntrospector.create(
				metadata, reflectionManager,
				ConfigurationPropertySource.fromMap( properties )
		);
	}

}
