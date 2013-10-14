/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.integration.jms;

import java.io.File;

import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.controller.RegistrationMdb;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;

/**
 * Create deployments for JMS Master/Slave configuration integration tests.
 * Make sure to test for a secured JMS environment.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
public final class DeploymentJmsMasterSlave {

	private DeploymentJmsMasterSlave() {
		//not allowed
	}

	public static Archive<?> createMaster(String deploymentName, int refreshPeriod, File tmpDir) throws Exception {
		return baseArchive( deploymentName, masterPersistenceXml( deploymentName, refreshPeriod, tmpDir ) )
				.addClass( RegistrationMdb.class )
				.addAsWebInfResource( hornetqJmsXml(), "hornetq-jms.xml" )
				;
	}

	public static Archive<?> createSlave(String deploymentName, int refreshPeriod, File tmpDir) throws Exception {
		return baseArchive( deploymentName, slavePersistenceXml( deploymentName, refreshPeriod, tmpDir ) );
	}

	private static WebArchive baseArchive(String name, PersistenceDescriptor unitDef) throws Exception {
		WebArchive webArchive = ShrinkWrap
				.create( WebArchive.class, name + ".war" )
				.addClasses( RegistrationController.class, RegisteredMember.class, RegistrationConfiguration.class, SearchNewEntityJmsMasterSlave.class )
				.addAsResource( new StringAsset( unitDef.exportAsString() ), "META-INF/persistence.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		return webArchive;
	}

	private static PersistenceDescriptor masterPersistenceXml(String name, int refreshPeriod, File tmpDir)
			throws Exception {
		return commonUnitDef( name, "filesystem-master", refreshPeriod, tmpDir ).up().up();
	}

	private static PersistenceDescriptor slavePersistenceXml(String name, int refreshPeriod, File tmpDir)
			throws Exception {
		return commonUnitDef( name, "filesystem-slave", refreshPeriod, tmpDir )
					.createProperty()
						.name( "hibernate.search.default.worker.backend" )
						.value( "jms" )
						.up()
					//We could use a Local ConnectionFactory but then we would bypass the authentication:
					//we actually want to verify we're able to authenticate
					.createProperty()
						.name( "hibernate.search.default.worker.jms.connection_factory" )
						.value( "RemoteConnectionFactory" )
						.up()
					.createProperty()
						.name( "hibernate.search.default.worker.jms.queue" )
						.value( RegistrationConfiguration.DESTINATION_QUEUE )
						.up()
					//Authentication credentials are specified in the AS7 configuration files
					//See properties files in server/standalone/configuration
					.createProperty()
						.name( "hibernate.search.default.worker.jms.login" )
						.value( "guest" )
						.up()
					.createProperty()
						.name( "hibernate.search.default.worker.jms.password" )
						.value( "password" )
						.up()
					.up()
				.up();
	}

	private static Properties<PersistenceUnit<PersistenceDescriptor>> commonUnitDef(
			String name, String directoryProvider, int refreshPeriod, File tmpDir) throws Exception {
		return Descriptors.create( PersistenceDescriptor.class )
				.createPersistenceUnit()
					.name( "pu-" + name )
					.jtaDataSource( "java:jboss/datasources/ExampleDS" )
					.getOrCreateProperties()
						.createProperty()
							.name( "hibernate.hbm2ddl.auto" )
							.value( "create-drop" )
							.up()
						.createProperty()
							.name( "hibernate.search.default.lucene_version" )
							.value( "LUCENE_CURRENT" )
							.up()
						.createProperty()
							.name( "hibernate.search.default.directory_provider" )
							.value( directoryProvider )
							.up()
						.createProperty()
							.name( "hibernate.search.default.sourceBase" )
							.value( tmpDir.getAbsolutePath() + "-sourceBase" )
							.up()
						.createProperty()
							.name( "hibernate.search.default.indexBase" )
							.value( tmpDir.getAbsolutePath() + "-" + name )
							.up()
						.createProperty()
							.name( "hibernate.search.default.refresh" )
							.value( String.valueOf( refreshPeriod ) )
							.up()
						.createProperty()
							.name( "hibernate.search.default.worker.execution" )
							.value( "sync" )
							.up();
	}

	private static Asset hornetqJmsXml() {
		String hornetqXml =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<messaging-deployment xmlns=\"urn:jboss:messaging-deployment:1.0\">"
				+ "<hornetq-server>"
					+ "<jms-destinations>"
						+ "<jms-queue name=\"hsearchQueue\">"
							+ "<entry name=\"" + RegistrationConfiguration.DESTINATION_QUEUE + "\"/>"
						+ "</jms-queue>"
					+ "</jms-destinations>"
				+ "</hornetq-server>"
			+ "</messaging-deployment>";
		return new StringAsset( hornetqXml );
	}
}
