/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter.impl;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.search.filter.FilterKey;

/**
 * A {@link FilterKey} based on the name of a filter definition and the name/value pairs passed to a given instantiation
 * of that definition.
 *
 * @author Gunnar Morling
 */
// The deprecated public type FilterKey is extended here to plug the new mechanism of automatic key determination into
// the existing key handling routine. DefaultFilterKey will be the one and only key type as of Hibernate Search 6.
@SuppressWarnings("deprecation")
public final class DefaultFilterKey extends FilterKey {

	private final String filterDefName;
	private final SortedMap<String, Object> parameters;

	public DefaultFilterKey(String filterDefName, Map<String, Object> parameters) {
		this.filterDefName = filterDefName;
		this.parameters = new TreeMap<>( parameters );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( parameters == null ) ? 0 : parameters.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( DefaultFilterKey.class != obj.getClass() ) {
			return false;
		}
		DefaultFilterKey other = (DefaultFilterKey) obj;
		if ( filterDefName == null ) {
			if ( other.filterDefName != null ) {
				return false;
			}
		}
		else if ( !filterDefName.equals( other.filterDefName ) ) {
			return false;
		}
		if ( parameters == null ) {
			if ( other.parameters != null ) {
				return false;
			}
		}
		else if ( !parameters.equals( other.parameters ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "DefaultFilterKey [filterDefName=" + filterDefName + ", parameters=" + parameters + "]";
	}
}
