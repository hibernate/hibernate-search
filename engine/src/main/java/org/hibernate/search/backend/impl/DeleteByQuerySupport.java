/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.spi.DeletionQuery;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This class provides means to convert all (by default) supported DeletionQueries back to Lucene Queries and to their
 * String[] representation and back.
 *
 * @author Martin Braun
 */
public final class DeleteByQuerySupport {

	private DeleteByQuerySupport() {
		// Not meant to be invoked
	}

	private static final Log log = LoggerFactory.make();

	public static DeletionQuery fromString(int queryKey, String[] string) {
		switch ( queryKey ) {
			case SingularTermDeletionQuery.QUERY_KEY:
				return SingularTermDeletionQuery.fromString( string );
			default:
				throw log.unknownDeletionQueryKeySpecified( queryKey );
		}
	}

	public static boolean isSupported(Class<? extends DeletionQuery> type) {
		if ( SingularTermDeletionQuery.class == type ) {
			return true;
		}
		return false;
	}

}
