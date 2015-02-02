/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Custom filter keys are deprecated and are scheduled for removal in Hibernate Search 6. As of Hibernate
 * Search 5.1, keys for caching Lucene filters are calculated automatically based on the given filter parameters.
 */
@Deprecated
public class StandardFilterKey extends FilterKey {
	private final List<Object> parameters = new ArrayList<Object>();
	private boolean implSet;

	@Override
	public void setImpl(Class<?> impl) {
		super.setImpl( impl );
		//add impl once and only once
		if ( implSet ) {
			parameters.set( 0, impl );
		}
		else {
			implSet = true;
			parameters.add( 0, impl );
		}
	}

	public void addParameter(Object value) {
		parameters.add( value );
	}

	@Override
	public int hashCode() {
		int hash = 23;
		for ( Object param : parameters ) {
			hash = 31 * hash + ( param != null ? param.hashCode() : 0 );
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof StandardFilterKey ) ) {
			return false;
		}
		StandardFilterKey that = (StandardFilterKey) obj;
		int size = parameters.size();
		if ( size != that.parameters.size() ) {
			return false;
		}
		for ( int index = 0; index < size; index++ ) {
			Object paramThis = parameters.get( index );
			Object paramThat = that.parameters.get( index );
			if ( paramThis == null && paramThat != null ) {
				return false;
			}
			if ( paramThis != null && !paramThis.equals( paramThat ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "StandardFilterKey [parameters=" + parameters + "]";
	}
}
