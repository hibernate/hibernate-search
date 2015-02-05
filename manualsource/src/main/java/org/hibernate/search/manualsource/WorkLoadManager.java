/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource;

import org.hibernate.search.manualsource.source.EntitySourceContext;

/**
 * Thread-safe.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface WorkLoadManager {
	WorkLoad createWorkLoad();

	/**
	 * Overrides the EntitySourceContext instance provided by the builder.
	 */
	WorkLoad createWorkLoad(EntitySourceContext entitySourceContext);

	void close();
}
