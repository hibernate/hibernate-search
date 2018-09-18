/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.spi;

import org.hibernate.service.Service;

/**
 * A service allowing to postpone Hibernate Search initialization works
 * to a later time, when the environment (e.g. provided services
 * such as {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry})
 * will be ready to accept requests.
 */
public interface EnvironmentSynchronizer extends Service {

	/**
	 * Run the given work as soon as the environment is
	 * deemed "ready" (exactly what "ready" means is
	 * implementation-dependent).
	 * <p>
	 * If the environment is already "ready", run the
	 * work now, synchronously.
	 *
	 * @param runnable The work to run.
	 */
	void whenEnvironmentReady(Runnable runnable);

	/**
	 * Run the given work just before the environment is
	 * destroyed (exactly what "destroyed" means is
	 * implementation-dependent).
	 * <p>
	 * If the environment is already "destroyed", run the
	 * work now, synchronously.
	 *
	 * @param runnable The work to run.
	 */
	void whenEnvironmentDestroying(Runnable runnable);
}
