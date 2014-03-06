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

import java.util.Map.Entry;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.test.integration.wildfly.PackagerHelper;
import org.hibernate.search.test.performance.scenario.TestScenario;
import org.hibernate.search.test.performance.scenario.TestScenarioFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Tomas Hradec
 */
@RunWith(Arquillian.class)
public class TestRunnerArquillian {

	private static final TestScenario scenario = TestScenarioFactory.create();

	public static final String RUNNER_PROPERTIES = "runner.properties";
	public static final String TARGET_DIR_KEY = "target";

	@Deployment
	public static Archive<?> createTestArchive() {
		WebArchive archive = ShrinkWrap
				.create( WebArchive.class, TestRunnerArquillian.class.getSimpleName() + ".war" )
				.addPackages( true, TestRunnerArquillian.class.getPackage() )
				.addClass( TestConstants.class )
				.addAsResource( createPersistenceXml(), "META-INF/persistence.xml" )
				.addAsLibraries( PackagerHelper.hibernateSearchLibraries() )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.add( manifest(), "META-INF/MANIFEST.MF" )
				.addAsWebInfResource( new StringAsset( TARGET_DIR_KEY + "=" + TestConstants.getTargetDir( TestRunnerArquillian.class ).getAbsolutePath() ), "classes/" + RUNNER_PROPERTIES );
		return archive;
	}

	private static Asset createPersistenceXml() {
		PersistenceDescriptor pd = Descriptors.create( PersistenceDescriptor.class ).version( "2.0" );

		PersistenceUnit<PersistenceDescriptor> pu = pd.createPersistenceUnit()
				.name( "primary" )
				.jtaDataSource( System.getProperty( "datasource", "java:jboss/datasources/ExampleDS" ) );

		Properties properties = scenario.getHibernateProperties();
		properties.setProperty( "hibernate.transaction.factory_class", "org.hibernate.engine.transaction.internal.jta.JtaTransactionFactory" );

		for ( Entry<Object, Object> property : properties.entrySet() ) {
			pu.getOrCreateProperties().
					createProperty().
					name( property.getKey().toString() ).
					value( property.getValue().toString() );
		}

		return new StringAsset( pd.exportAsString() );
	}

	public static Asset manifest() {
		String manifest = Descriptors.create( ManifestDescriptor.class )
				.attribute( "Dependencies", "org.apache.commons.lang" )
				.exportAsString();
		return new StringAsset( manifest );
	}

	@PersistenceContext
	private EntityManager em;

	@Test
	public void runPerformanceTest() {
		SessionFactory sf = em.unwrap( Session.class ).getSessionFactory();
		scenario.run( sf );
	}

}
