/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.logging.spi.FailureCollector;
import org.hibernate.search.engine.environment.service.spi.ServiceManager;

class RootBuildContext {

	private final ServiceManager serviceManager;
	private final FailureCollector failureCollector;

	RootBuildContext(ServiceManager serviceManager, FailureCollector failureCollector) {
		this.serviceManager = serviceManager;
		this.failureCollector = failureCollector;
	}

	ServiceManager getServiceManager() {
		return serviceManager;
	}

	FailureCollector getFailureCollector() {
		return failureCollector;
	}
}
