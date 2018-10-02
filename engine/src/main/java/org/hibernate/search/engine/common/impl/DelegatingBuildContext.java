/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.environment.service.spi.ServiceManager;

class DelegatingBuildContext {
	private final RootBuildContext delegate;

	DelegatingBuildContext(RootBuildContext delegate) {
		this.delegate = delegate;
	}

	public ServiceManager getServiceManager() {
		return delegate.getServiceManager();
	}

}
