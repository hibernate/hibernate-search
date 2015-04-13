/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.SingularTermDeletionQuery;
import org.hibernate.search.exception.AssertionFailure;

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

	private static final DeletionQueryMapper[] MAPPERS;
	static {
		{
			Map<Integer, DeletionQueryMapper> map = new HashMap<>();

			map.put( SingularTermDeletionQuery.QUERY_KEY, new SingularTermDeletionQueryMapper() );

			MAPPERS = new DeletionQueryMapper[map.size()];
			for ( Map.Entry<Integer, DeletionQueryMapper> entry : map.entrySet() ) {
				MAPPERS[entry.getKey()] = entry.getValue();
			}
		}
	}

	public static DeletionQueryMapper getMapper(int queryKey) {
		return MAPPERS[queryKey];
	}

	private static final Set<Class<? extends DeletionQuery>> SUPPORTED_TYPES;
	static {
		{
			Map<Integer, Class<? extends DeletionQuery>> map = new HashMap<>();

			map.put( SingularTermDeletionQuery.QUERY_KEY, SingularTermDeletionQuery.class );

			SUPPORTED_TYPES = Collections.unmodifiableSet( new HashSet<>( map.values() ) );
		}
	}

	public static boolean isSupported(Class<? extends DeletionQuery> type) {
		return SUPPORTED_TYPES.contains( type );
	}

	static {
		// make sure everything is setup correctly
		Set<Integer> counts = new HashSet<>();
		counts.add( MAPPERS.length );
		counts.add( SUPPORTED_TYPES.size() );
		if ( counts.size() != 1 ) {
			throw new AssertionFailure( "all Maps/Sets inside this class must have the same "
					+ "size. Make sure that every QueryType is found in every Map/Set" );
		}
	}

	/*
	 * Why we use String arrays here: Why use Strings at all and not i.e. byte[] ?: As we want to support different
	 * versions of Queries later on and we don't want to write Serialization code over and over again in the
	 * Serialization module for all the different types we have one toString and fromString here. and we force to use
	 * Strings because i.e. using byte[] would suggest using Java Serialization for this process, but that would prevent
	 * us from changing our API internally in the future. Why not plain Strings?: But using plain Strings would leave us
	 * with yet another problem: We would have to encode all our different fields into a single string. For that we
	 * would need some magical chars to separate or we would need to use something like JSON/XML to pass the data
	 * consistently. This would be far too much overkill for us. By using String[] we force users (or API designers) to
	 * no use Java Serialization (well Serialization to BASE64 or another String representation is still possible, but
	 * nvm that) and we still have an _easy_ way of dealing with multiple fields in our different Query Types.
	 */

}
