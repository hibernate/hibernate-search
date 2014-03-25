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
			worker = new TransactionalWorker();
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
