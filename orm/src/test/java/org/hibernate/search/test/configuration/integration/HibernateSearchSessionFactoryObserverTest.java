/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.integration;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.BytemanHelper.BytemanAccessor;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@RunWith(BMUnitRunner.class)
public class HibernateSearchSessionFactoryObserverTest {

	@Rule
	public BytemanAccessor byteman = BytemanHelper.createAccessor();

	@Test
	@BMRules(rules = {
			@BMRule(targetClass = "org.hibernate.internal.SessionFactoryImpl",
					targetMethod = "close",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "countInvocation()",
					name = "Session close counter"),
			@BMRule(targetClass = "org.hibernate.search.spi.SearchIntegratorBuilder",
					targetMethod = "buildSearchIntegrator",
					action = "throw new java.lang.RuntimeException(\"Byteman created runtime exception\")",
					name = "Factory build prohibitor")

	})
	public void testSessionFactoryGetsClosedOnSearchFactoryCreationFailure() {
		final FullTextSessionBuilder builder = new FullTextSessionBuilder();
		builder.addAnnotatedClass( Foo.class );
		try {
			builder.build();
			fail( "ByteMan should have forced an exception" );
		}
		catch (RuntimeException e) {
			assertEquals( "Wrong invocation count", 1, byteman.getAndResetInvocationCount() );
		}
	}

	@Entity
	@Indexed
	public static class Foo {
		@Id
		private long id;
	}
}

