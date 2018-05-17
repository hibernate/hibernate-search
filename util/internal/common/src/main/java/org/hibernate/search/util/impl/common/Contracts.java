/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.impl.common.logging.Log;

public final class Contracts {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private Contracts() {
	}

	public static void assertNotNull(Object object, String objectDescription) {
		if ( object == null ) {
			throw log.mustNotBeNull( objectDescription );
		}
	}
}
