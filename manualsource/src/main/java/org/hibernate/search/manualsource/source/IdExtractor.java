/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.source;

import java.io.Serializable;

/**
 * Get an id from a given entity.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
//TODO the alternative is to have WorkLoad methods accept ids
public interface IdExtractor {

	/**
	 * Get the id for that entity
	 */
	Serializable getId(Object entity);
}
