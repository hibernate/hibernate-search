/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;

/**
 * An object representic Elasticsearch type mappings.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html#mapping-type
 * @author Yoann Rodiere
 */
public class TypeMapping {

	/**
	 * Must be null when we don't want to include it in JSON serialization.
	 */
	private Map<String, PropertyMapping> properties;

	private DynamicType dynamic;

	public Map<String, PropertyMapping> getProperties() {
		return properties == null ? null : Collections.unmodifiableMap( properties );
	}

	private Map<String, PropertyMapping> getInitializedProperties() {
		if ( properties == null ) {
			properties = new TreeMap<>();
		}
		return properties;
	}

	public void addProperty(String name, PropertyMapping mapping) {
		getInitializedProperties().put(name, mapping);
	}

	public void removeProperty(String name) {
		getInitializedProperties().remove( name );
	}

	public DynamicType getDynamic() {
		return dynamic;
	}

	public void setDynamic(DynamicType dynamic) {
		this.dynamic = dynamic;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

}
