/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration.integration;

import java.util.Properties;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.util.BytemanHelper;
import org.hibernate.search.test.util.ServiceRegistryTools;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@RunWith(BMUnitRunner.class)
public class HibernateSearchSessionFactoryObserverTest {

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.internal.SessionFactoryImpl",
					targetMethod = "close",
					helper = "org.hibernate.search.test.util.BytemanHelper",
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


