/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.jmx;

import java.io.File;

import org.hibernate.search.Environment;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.test.util.ManualConfiguration;

import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
public class MutableSearchFactoryAndJMXTest {

	@Test
	public void testRebuildFactory() {
		File targetDir = TestConstants.getTargetDir( MutableSearchFactoryAndJMXTest.class );
		File simpleJndiDir = new File( targetDir, "simpleJndi" );
		simpleJndiDir.mkdir();

		ManualConfiguration configuration = new HibernateManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", "ram" )
				.addProperty( "hibernate.session_factory_name", "java:comp/SessionFactory" )
				.addProperty( "hibernate.jndi.class", "org.osjava.sj.SimpleContextFactory" )
				.addProperty( "hibernate.jndi.org.osjava.sj.root", simpleJndiDir.getAbsolutePath() )
				.addProperty( "hibernate.jndi.org.osjava.sj.jndi.shared", "true" )
				.addProperty( Environment.JMX_ENABLED, "true" );

		new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();

		// if there are problems with the JMX registration there will be an exception when the new factory is build
		new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
	}
}


