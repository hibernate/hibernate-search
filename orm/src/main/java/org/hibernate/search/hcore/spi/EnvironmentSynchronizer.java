/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.spi;

import org.hibernate.service.Service;

/**
 * A service allowing to postpone Hibernate initialization works
 * to a later time, when the environment (e.g. provided services
 * such as {@link BeanResolver}) will be ready to accept requests.
 *
 * @hsearch.experimental This type is under active development.
 *    You should be prepared for incompatible changes in future releases.
 * @since 5.8
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
	 * @param runnable
	 */
	void whenEnvironmentReady(Runnable runnable);

}
