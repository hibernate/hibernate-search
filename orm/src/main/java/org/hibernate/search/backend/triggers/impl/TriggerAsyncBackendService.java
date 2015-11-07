/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.triggers.impl;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;

/**
 * Created by Martin on 14.11.2015.
 * <p>
 * Service that controls the trigger based asynchronous index updating mechanism
 * <p>
 * Does not implement Startable and Stoppable as its lifecycle needs to be controlled
 * explicitly
 */
public interface TriggerAsyncBackendService extends Service {

	void start(
			SessionFactory sessionFactory,
			ExtendedSearchIntegrator extendedSearchIntegrator,
			ClassLoaderService cls,
			Properties properties);

	void stop();

}
