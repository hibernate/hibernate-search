/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.session.context.spi;


/**
 * Provides visibility from the lower layers of Hibernate Search (engine, backend)
 * to the session defined in the upper layers (mapping).
 */
// TODO add more in here? We could add default query timeouts, default query hints, etc.
public interface SessionContextImplementor {

	String getTenantIdentifier();

}
