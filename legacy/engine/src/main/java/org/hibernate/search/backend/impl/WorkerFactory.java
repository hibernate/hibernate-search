/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.Map;
import java.util.Properties;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * @author Emmanuel Bernard
 */
public abstract class WorkerFactory {

	public static Worker createWorker(SearchConfiguration searchConfiguration,
			WorkerBuildContext buildContext,
			QueueingProcessor queueingProcessor) {
		Properties properties = getProperties( searchConfiguration );
		String workerImplClassName = properties.getProperty( Environment.WORKER_SCOPE );
		Worker worker;
		if ( StringHelper.isEmpty( workerImplClassName ) || "transaction".equalsIgnoreCase( workerImplClassName ) ) {
			worker = new PerTransactionWorker();
		}
		else {
			worker = instantiateExplicitlyConfiguredWorker( buildContext, workerImplClassName );
		}
		worker.initialize( properties, buildContext, queueingProcessor );
		return worker;
	}

	private static Worker instantiateExplicitlyConfiguredWorker(WorkerBuildContext buildContext, String workerImplClassName) {
		ServiceManager serviceManager = buildContext.getServiceManager();
		return ClassLoaderHelper.instanceFromName(
				Worker.class,
				workerImplClassName,
				"worker",
				serviceManager
		);
	}

	private static Properties getProperties(SearchConfiguration searchConfiguration) {
		Properties props = searchConfiguration.getProperties();
		Properties workerProperties = new Properties();
		for ( Map.Entry entry : props.entrySet() ) {
			String key = (String) entry.getKey();
			if ( key.startsWith( "hibernate.search.worker" ) ) {
				workerProperties.setProperty( key, (String) entry.getValue() );
			}
		}
		return workerProperties;
	}
}
