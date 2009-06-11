// $Id$
package org.hibernate.search.query;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.FullTextFilter;
import org.hibernate.search.filter.FullTextFilterImplementor;

/**
 * @author Emmanuel Bernard
 */
public class FullTextFilterImpl implements FullTextFilterImplementor {
	private final Map<String, Object> parameters = new HashMap<String, Object>();
	private String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public FullTextFilter setParameter(String name, Object value) {
		parameters.put( name, value );
		return this;
	}

	public Object getParameter(String name) {
		return parameters.get( name );
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}
}
