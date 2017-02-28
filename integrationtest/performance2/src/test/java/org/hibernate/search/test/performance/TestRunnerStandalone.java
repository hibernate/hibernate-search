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
		setDefaultProperty( properties, "hibernate.connection.driver_class", "org.h2.Driver" );
		setDefaultProperty( properties, "hibernate.connection.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1" );
		setDefaultProperty( properties, "hibernate.connection.username", "sa" );
		setDefaultProperty( properties, "hibernate.connection.password", "" );
		setDefaultProperty( properties, "hibernate.c3p0.min_size", "5" );
		setDefaultProperty( properties, "hibernate.c3p0.max_size", "20" );
		setDefaultProperty( properties, "hibernate.c3p0.max_statements", "50" );
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
