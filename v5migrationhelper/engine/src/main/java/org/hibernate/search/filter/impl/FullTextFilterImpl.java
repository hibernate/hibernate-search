/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.filter.FullTextFilter;
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

	@Override
	public String getName() {
		return name;
	}

	@Override
	public FullTextFilter setParameter(String name, Object value) {
		parameters.put( name, value );
		return this;
	}

	@Override
	public Object getParameter(String name) {
		return parameters.get( name );
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return "FullTextFilterImpl [name=" + name + ", parameters=" + parameters + "]";
	}
}
