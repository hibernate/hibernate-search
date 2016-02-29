/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.util.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.search.exception.AssertionFailure;

/**
 * Utility class that allows you to access multiple JPA queries at once. Data is retrieved from the database in batches
 * and ordered by a given comparator. No need for messy Unions on the database level! <br>
 * <br>
 * This is particularly useful if you scroll all the data from the database incrementally and if you can compare in
 * Code.
 *
 * @author Martin
 */
public class MultiQueryAccess {

	private final Map<String, Long> currentCountMap;
	private final Map<String, QueryWrapper> queryMap;
	private final Comparator<ObjectIdentifierWrapper> comparator;
	private final int batchSize;

	private final Map<String, Long> currentPosition;
	private final Map<String, LinkedList<Object>> values;

	private Object scheduled;
	private String identifier;

	/**
	 * this doesn't do real batching as it has a batchSize of 1
	 */
	public MultiQueryAccess(
			Map<String, Long> countMap,
			Map<String, QueryWrapper> queryMap,
			Comparator<ObjectIdentifierWrapper> comparator) {
		this( countMap, queryMap, comparator, 1 );
	}

	/**
	 * this does batching
	 */
	public MultiQueryAccess(
			Map<String, Long> countMap,
			Map<String, QueryWrapper> queryMap,
			Comparator<ObjectIdentifierWrapper> comparator,
			int batchSize) {
		if ( countMap.size() != queryMap.size() ) {
			throw new IllegalArgumentException( "countMap.size() must be equal to queryMap.size()" );
		}
		this.currentCountMap = countMap;
		this.queryMap = queryMap;
		this.comparator = comparator;
		this.batchSize = batchSize;
		this.currentPosition = new HashMap<>();
		this.values = new HashMap<>();
		for ( String ident : queryMap.keySet() ) {
			this.values.put( ident, new LinkedList<>() );
			this.currentPosition.put( ident, 0L );
		}
	}

	private static int toInt(Long l) {
		return (int) (long) l;
	}

	/**
	 * increments the value to be returned by {@link #get()}
	 *
	 * @return true if there is a value left to be visited in the database
	 */
	public boolean next() {
		this.scheduled = null;
		this.identifier = null;
		List<ObjectIdentifierWrapper> tmp = new ArrayList<>( this.queryMap.size() );
		for ( Map.Entry<String, QueryWrapper> entry : this.queryMap.entrySet() ) {
			String identifier = entry.getKey();
			QueryWrapper query = entry.getValue();
			if ( !this.currentCountMap.get( identifier ).equals( 0L ) ) {
				if ( this.values.get( identifier ).size() == 0 ) {
					// the last batch is empty. get a new one
					Long processed = this.currentPosition.get( identifier );
					// yay JPA...
					query.setFirstResult( toInt( processed ) );
					query.setMaxResults( this.batchSize );
					@SuppressWarnings("unchecked")
					List<Object> list = query.getResultList();
					this.values.get( identifier ).addAll( list );
				}
				Object val = this.values.get( identifier ).getFirst();
				tmp.add( new ObjectIdentifierWrapper( val, identifier ) );
			}
		}
		tmp.sort( this.comparator );
		if ( tmp.size() > 0 ) {
			ObjectIdentifierWrapper arr = tmp.get( 0 );
			this.scheduled = arr.object;
			this.identifier = arr.identifier;
			this.values.get( this.identifier ).pop();
			Long currentPosition = this.currentPosition.get( arr.identifier );
			Long newCurrentPosition = this.currentPosition.computeIfPresent( arr.identifier, (clazz, old) -> old + 1 );
			if ( Math.abs( newCurrentPosition - currentPosition ) != 1L ) {
				throw new AssertionFailure( "the new currentPosition count should be exactly 1 " + "greater than the old one" );
			}
			Long count = this.currentCountMap.get( arr.identifier );
			Long newCount = this.currentCountMap.computeIfPresent(
					arr.identifier, (clazz, old) -> old - 1
			);
			if ( Math.abs( count - newCount ) != 1L ) {
				throw new AssertionFailure( "the new old remaining count should be exactly 1 " + "greater than the new one" );
			}
		}
		return this.scheduled != null;
	}

	/**
	 * @return the current value
	 */
	public Object get() {
		if ( this.scheduled == null ) {
			throw new IllegalStateException( "either empty or next() has not been called" );
		}
		return this.scheduled;
	}

	/**
	 * @return the identifier of the current value
	 */
	public String identifier() {
		if ( this.identifier == null ) {
			throw new IllegalStateException( "either empty or next() has not been called" );
		}
		return this.identifier;
	}

	public static class ObjectIdentifierWrapper {

		public final Object object;
		public final String identifier;

		public ObjectIdentifierWrapper(Object object, String identifier) {
			super();
			this.object = object;
			this.identifier = identifier;
		}

	}

}
