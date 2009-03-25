// $Id$
package org.hibernate.search.filter;

import java.util.List;
import java.util.ArrayList;

/**
 * Implements a filter key usign all injected parameters to compute
 * equals and hashCode
 * the order the parameters are added is significant
 *
 * @author Emmanuel Bernard
 */
public class StandardFilterKey extends FilterKey {
	private final List parameters = new ArrayList();
	private boolean implSet;


	public void setImpl(Class impl) {
		super.setImpl( impl );
		//add impl once and only once
		if (implSet) {
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
	public int hashCode() {
		int hash = 23;
		for (Object param : parameters) {
			hash = 31*hash + (param != null ? param.hashCode() : 0);
		}
		return hash;
	}

	public boolean equals(Object obj) {
		if ( ! ( obj instanceof StandardFilterKey ) ) return false;
		StandardFilterKey that = (StandardFilterKey) obj;
		int size = parameters.size();
		if ( size != that.parameters.size() ) return false;
		for (int index = 0 ; index < size; index++) {
			Object paramThis = parameters.get( index );
			Object paramThat = that.parameters.get( index );
			if (paramThis == null && paramThat != null) return false;
			if (paramThis != null && ! paramThis.equals( paramThat ) ) return false;
		}
		return true;
	}
}
