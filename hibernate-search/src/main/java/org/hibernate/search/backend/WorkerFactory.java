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
package org.hibernate.search.backend;

import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.TransactionalWorker;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.ClassLoaderHelper;

/**
 * @author Emmanuel Bernard
 */
public abstract class WorkerFactory {

	private static Properties getProperties(SearchConfiguration cfg) {
		Properties props = cfg.getProperties();
		Properties workerProperties = new Properties();
		for ( Map.Entry entry : props.entrySet() ) {
			String key = (String) entry.getKey();
			if ( key.startsWith( Environment.WORKER_PREFIX ) ) {
				workerProperties.setProperty( key, (String) entry.getValue() );
			}
		}
		return workerProperties;
	}

	public static Worker createWorker(SearchConfiguration cfg, WorkerBuildContext context) {
		Properties props = getProperties( cfg );
		String impl = props.getProperty( Environment.WORKER_SCOPE );
		Worker worker;
		if ( StringHelper.isEmpty( impl ) ) {
			worker = new TransactionalWorker();
		}
		else if ( "transaction".equalsIgnoreCase( impl ) ) {
			worker = new TransactionalWorker();
		}
		else {
			worker = ClassLoaderHelper.instanceFromName(
					Worker.class,
					impl, WorkerFactory.class, "worker"
			);
		}
		worker.initialize( props, context );
		return worker;
	}
}
