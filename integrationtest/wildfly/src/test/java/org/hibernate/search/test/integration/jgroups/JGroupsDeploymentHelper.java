/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jgroups;

import static org.hibernate.search.test.integration.VersionTestHelper.addDependencyToSearchModule;

import java.io.File;

import org.hibernate.search.test.integration.VersionTestHelper;
import org.hibernate.search.test.integration.jms.SearchNewEntityJmsMasterSlave;
import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;

/**
 * Create deployments using the JGroups backend
 *
 * @author Sanne Grinovero
 */
public class JGroupsDeploymentHelper {

	private JGroupsDeploymentHelper() {
		//not allowed
	}

	public static Archive<?> createMaster(String deploymentName, int refreshPeriodInSec, File tmpDir) throws Exception {
		WebArchive master = baseArchive( deploymentName, masterPersistenceXml( deploymentName, refreshPeriodInSec, tmpDir ) );
		addDependencyToSearchModule( master );
		return master;
	}

	public static Archive<?> createSlave(String deploymentName, int refreshPeriodInSec, File tmpDir) throws Exception {
		WebArchive slave = baseArchive( deploymentName, slavePersistenceXml( deploymentName, refreshPeriodInSec, tmpDir ) );
		addDependencyToSearchModule( slave );
		return slave;
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
		return commonUnitDef( name, "filesystem-master", refreshPeriod, tmpDir )
				.createProperty()
					.name( "hibernate.search.default.worker.backend" )
					.value( "jgroupsMaster" )
					.up()
				.up()
			.up();
	}

	private static PersistenceDescriptor slavePersistenceXml(String name, int refreshPeriod, File tmpDir)
			throws Exception {
		return commonUnitDef( name, "filesystem-slave", refreshPeriod, tmpDir )
					.createProperty()
						.name( "hibernate.search.default.worker.backend" )
						.value( "jgroupsSlave" )
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
					// The deployment Scanner is disabled as the JipiJapa integration is not available because of the custom Hibernate ORM module:
					.clazz( RegisteredMember.class.getName() )
					.getOrCreateProperties()
						.createProperty()
							.name( "jboss.as.jpa.providerModule" )
							.value( VersionTestHelper.injectVariables( "org.hibernate.search.hibernate-orm-repackage:${dependency.version.HibernateSearch}" ) )
							.up()
						.createProperty()
							.name( "hibernate.transaction.jta.platform" )
							.value( "org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform" )
							.up()
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

}
