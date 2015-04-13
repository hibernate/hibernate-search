/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.SingularTermDeletionQuery;

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

	private static final int MAPPER_COUNT = 1;

	private static final Lock MAPPERS_LOCK = new ReentrantLock();
	private static DeletionQueryMapper[] MAPPERS;

	public static DeletionQueryMapper getMapper(int queryKey) {
		if ( MAPPERS == null ) {
			// LAZY init, but don't do it more than once
			MAPPERS_LOCK.lock();
			try {
				if ( MAPPERS == null ) {
					DeletionQueryMapper[] arr = new DeletionQueryMapper[MAPPER_COUNT];
					arr[SingularTermDeletionQuery.QUERY_KEY] = new SingularTermDeletionQueryMapper();
					MAPPERS = arr;
				}
			}
			finally {
				MAPPERS_LOCK.unlock();
			}
		}
		return MAPPERS[queryKey];
	}

	private static final Lock SUPPORTED_TYPES_LOCK = new ReentrantLock();
	private static Set<Class<? extends DeletionQuery>> SUPPORTED_TYPES;

	public static boolean isSupported(Class<? extends DeletionQuery> type) {
		if ( SUPPORTED_TYPES == null ) {
			// LAZY init, but don't to it more than once
			SUPPORTED_TYPES_LOCK.lock();
			try {
				if ( SUPPORTED_TYPES == null ) {
					Set<Class<? extends DeletionQuery>> set = new HashSet<>();
					set.add( SingularTermDeletionQuery.class );
					SUPPORTED_TYPES = Collections.unmodifiableSet( set );
				}
			}
			finally {
				SUPPORTED_TYPES_LOCK.unlock();
			}
		}
		return SUPPORTED_TYPES.contains( type );
	}

}
