/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl.model;

import java.util.Map;
import java.util.TreeMap;

/**
 * An object representic an Elasticsearch index.
 *
 * @author Yoann Rodiere
 */
public class IndexMetadata {

	private String name;

	private Map<String, TypeMapping> mappings = new TreeMap<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, TypeMapping> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, TypeMapping> mappings) {
		this.mappings = mappings;
	}

	public void putMapping(String name, TypeMapping mapping) {
		this.mappings.put( name, mapping );
	}

	public void removeMapping(String name) {
		this.mappings.remove( name );
	}

}
