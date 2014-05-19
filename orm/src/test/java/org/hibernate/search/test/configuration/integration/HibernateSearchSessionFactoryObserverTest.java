/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.integration;

import java.util.Properties;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.test.util.ServiceRegistryTools;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@RunWith(BMUnitRunner.class)
public class HibernateSearchSessionFactoryObserverTest {

	//Disabled: see HSEARCH-1600
	@Test @Ignore
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.internal.SessionFactoryImpl",
					targetMethod = "close",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "countInvocation()",
					name = "Session close counter"),
			@BMRule(targetClass = "org.hibernate.search.spi.SearchFactoryBuilder",
					targetMethod = "buildSearchFactory",
					action = "throw new java.lang.RuntimeException(\"Byteman created runtime exception\")",
					name = "Factory build prohibitor")

	})
	public void testSessionFactoryGetsClosedOnSearchFactoryCreationFailure() {
		Configuration hibernateConfiguration = new Configuration();
		// we need at least entity, otherwise the observer does not get registered at all
		hibernateConfiguration.addAnnotatedClass( Foo.class );
		Properties properties = new Properties();
		properties.setProperty( "hibernate.search.default.directory_provider", "ram" );
		hibernateConfiguration.getProperties().putAll( properties );

		ServiceRegistryBuilder registryBuilder = new ServiceRegistryBuilder();
		registryBuilder.applySettings( hibernateConfiguration.getProperties() );

		ServiceRegistry serviceRegistry = ServiceRegistryTools.build( registryBuilder );
		try {
			hibernateConfiguration.buildSessionFactory( serviceRegistry );
			fail( "ByteMan should have forced an exception" );
		}
		catch (RuntimeException e) {
			assertEquals( "Wrong invocation count", 1, BytemanHelper.getAndResetInvocationCount() );
		}
	}

	@Entity
	@Indexed
	public static class Foo {
		@Id
		private long id;
	}
}


