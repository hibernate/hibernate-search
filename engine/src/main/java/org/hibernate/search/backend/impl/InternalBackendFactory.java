/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.Properties;

import org.hibernate.search.backend.impl.blackhole.BlackHoleBackend;
import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Factory to instantiate the correct Search backend or to be more concrete the {@link BackendQueueProcessor} implementation.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public final class InternalBackendFactory {
	private static final Log log = LoggerFactory.make();

	// TODO - Consider using service mechanism instead of reflection to instantiate backends
	// need to allow multiple service impl first and provide a way for selecting one (HF)
	private static final String JMS_BACKEND = "org.hibernate.search.backend.jms.impl.JndiJMSBackend";
	private static final String JGROUPS_AUTO_BACKEND = "org.hibernate.search.backend.jgroups.impl.JGroupsBackend";
	private static final String JGROUPS_MASTER_BACKEND = "org.hibernate.search.backend.jgroups.impl.JGroupsMasterBackend";
	private static final String JGROUPS_SLAVE_BACKEND = "org.hibernate.search.backend.jgroups.impl.JGroupsSlaveBackend";

	private InternalBackendFactory() {
		//not allowed
	}

	public static Backend createBackend(String backendName, String indexName,
			Properties properties, WorkerBuildContext buildContext) {
		Backend backend;

		if ( StringHelper.isEmpty( backendName ) || "local".equalsIgnoreCase( backendName ) ) {
			backend = LocalBackend.INSTANCE;
		}
		else if ( "lucene".equalsIgnoreCase( backendName ) ) {
			log.deprecatedBackendName();
			backend = LocalBackend.INSTANCE;
		}
		else if ( "jms".equalsIgnoreCase( backendName ) ) {
			backend = ClassLoaderHelper.instanceFromName(
					Backend.class,
					JMS_BACKEND,
					"JMS backend",
					buildContext.getServiceManager()
			);
		}
		else if ( "blackhole".equalsIgnoreCase( backendName ) ) {
			backend = BlackHoleBackend.INSTANCE;
		}
		else if ( "jgroupsMaster".equals( backendName ) ) {
			backend = createJGroupsBackend( JGROUPS_MASTER_BACKEND, buildContext );
		}
		else if ( "jgroupsSlave".equals( backendName ) ) {
			backend = createJGroupsBackend( JGROUPS_SLAVE_BACKEND, buildContext );
		}
		else if ( "jgroups".equals( backendName ) ) {
			backend = createJGroupsBackend( JGROUPS_AUTO_BACKEND, buildContext );
		}
		else {
			ServiceManager serviceManager = buildContext.getServiceManager();
			try {
				backend = ClassLoaderHelper.instanceFromName(
						Backend.class,
						backendName,
						"Backend",
						serviceManager
				);
			}
			catch (SearchException backendException) {
				// Fall back: maybe it is a BackendQueueProcessor class?
				try {
					Class<? extends BackendQueueProcessor> backendQueueProcessorClass =
							ClassLoaderHelper.classForName(
									BackendQueueProcessor.class,
									backendName,
									"BackendQueueProcessor",
									serviceManager
							);
					backend = new ReflectionBasedBackend( backendQueueProcessorClass );
				}
				catch (SearchException backendQueueProcessorException) {
					backendException.addSuppressed( backendQueueProcessorException );
					throw backendException;
				}
			}
		}

		boolean enlistInTransaction = ConfigurationParseHelper.getBooleanValue(
				properties,
				Environment.WORKER_ENLIST_IN_TRANSACTION,
				false
		);
		if ( enlistInTransaction && !backend.isTransactional() ) {
			// We are expecting to use a transactional worker but the backend is not
			// this is war!
			backendName = StringHelper.isEmpty( backendName ) ? "lucene" : backendName;
			throw log.backendNonTransactional( indexName, backendName );

		}

		backend.initialize( properties, buildContext );

		return backend;
	}

	/**
	 * @param properties the configuration to parse
	 *
	 * @return true if the configuration uses sync indexing
	 */
	public static boolean isConfiguredAsSync(Properties properties) {
		// default to sync if none defined
		return !"async".equalsIgnoreCase( properties.getProperty( Environment.WORKER_EXECUTION ) );
	}

	private static Backend createJGroupsBackend(String backendClassName, WorkerBuildContext context) {
		try {
			return ClassLoaderHelper.instanceFromName(
					Backend.class,
					backendClassName,
					"JGroups backend",
					context.getServiceManager()
			);
		}
		catch (Exception e) {
			throw log.getUnableToCreateJGroupsBackendException( e );
		}
	}

}
