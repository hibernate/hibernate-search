/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.source;

/**
 * Interface implemented for the third-party object source.
 * Offers a way to typically pass datasource connection or CRUD interface.
 * This context instance is provided to methods that need such info, in particular object loading mechanisms.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface EntitySourceContext {

	/**
	 * Allows to nwrap datasource specific classes.
	 */
	<T> T unwrap(Class<T> cls);
}
