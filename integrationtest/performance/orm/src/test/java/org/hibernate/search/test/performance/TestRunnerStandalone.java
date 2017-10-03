/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance;

import java.io.IOException;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.test.performance.model.Author;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestContext;
import org.hibernate.search.test.performance.scenario.TestScenario;
import org.hibernate.search.test.performance.scenario.TestScenarioFactory;

import org.junit.Test;

/**
 * @author Tomas Hradec
 */
public class TestRunnerStandalone {

	private final TestScenario scenario = TestScenarioFactory.create();

	private SessionFactory getSessionFactory() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Author.class );
		cfg.addAnnotatedClass( Book.class );
		cfg.addProperties( getHibernateProperties() );
		return cfg.buildSessionFactory();
	}

	private Properties getHibernateProperties() {
		// Hibernate will also source properties from hibernate.properties
		return scenario.getHibernateProperties();
	}

	@Test
	public void runPerformanceTest() throws IOException {
		SessionFactory sf = getSessionFactory();
		try {
			scenario.run( new TestContext( sf ) );
		}
		finally {
			sf.close();
		}
	}

}
