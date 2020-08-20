/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.spi;

/**
 * A auto-closable reference to a {@link Service}.
 *
 * @author Gunnar Morling
 */
public class ServiceReference<S extends Service> implements AutoCloseable {

	private final ServiceManager serviceManager;
	private final Class<S> serviceType;
	private final S service;

	public ServiceReference(ServiceManager serviceManager, Class<S> serviceType) {
		this.serviceManager = serviceManager;
		this.serviceType = serviceType;
		this.service = serviceManager.requestService( serviceType );
	}

	public S get() {
		return service;
	}

	@Override
	public void close() {
		serviceManager.releaseService( serviceType );
	}
}
