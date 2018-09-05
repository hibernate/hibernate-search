/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.spi;

/**
 * Lifecycle contract for services which wish to be notified when it is time to stop.
 *
 * @author Hardy Ferentschik
 */
public interface Stoppable {
	/**
	 * Stop phase notification
	 */
	void stop();
}


