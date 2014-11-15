/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance;

import static org.hibernate.search.test.performance.util.Util.setDefaultProperty;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.test.performance.model.Author;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestScenario;
import org.hibernate.search.test.performance.scenario.TestScenarioFactory;
import org.junit.Test;

/**
 * @author Tomas Hradec
 */
@SuppressWarnings("deprecation")
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
		Properties properties = scenario.getHibernateProperties();
		setDefaultProperty( properties, "hibernate.dialect", "org.hibernate.dialect.H2Dialect" );
		setDefaultProperty( properties, "hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider" );
		setDefaultProperty( properties, "hibernate.hikari.minimumPoolSize", "5" );
		setDefaultProperty( properties, "hibernate.hikari.maximumPoolSize", "20" );
		setDefaultProperty( properties, "hibernate.hikari.dataSourceClassName", "org.h2.jdbcx.JdbcDataSource" );
		setDefaultProperty( properties, "hibernate.hikari.dataSource.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MULTI_THREADED=1" );
		setDefaultProperty( properties, "hibernate.hikari.dataSource.user", "sa" );
		setDefaultProperty( properties, "hibernate.hikari.dataSource.password", "" );
		setDefaultProperty( properties, "hibernate.connection.isolation", "TRANSACTION_READ_COMMITTED" );
		return properties;
	}

	@Test
	public void runPerformanceTest() {
		SessionFactory sf = getSessionFactory();
		try {
			scenario.run( sf );
		}
		finally {
			sf.close();
		}
	}

}
