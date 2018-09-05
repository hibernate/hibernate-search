/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.named.spi;

import org.hibernate.search.engine.service.spi.Service;

/**
 * The service to use to lookup objects by name.
 * For example this might delegate to a JNDI lookup service.
 * Abstracted as a Service to allow plugging how such a lookup
 * should be performed; for example to delegate to Hibernate ORM's
 * org.hibernate.engine.jndi.spi.JndiService
 */
public interface NamedResolver extends Service {

	/**
	 * Locate an object in {@literal JNDI} by name
	 *
	 * @param jndiName The {@literal JNDI} name of the object to locate
	 *
	 * @return The object found (may be null).
	 */
	Object locate(String jndiName);

}
