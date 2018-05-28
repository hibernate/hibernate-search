/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl.esnative;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * An object representing Elasticsearch type mappings.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html#mapping-type
 * @author Yoann Rodiere
 */
/*
 * CAUTION: JSON serialization is controlled by a specific adapter, which must be
 * updated whenever fields of this class are added, renamed or removed.
 */
public abstract class AbstractTypeMapping {

	private DynamicType dynamic;

	/**
	 * Must be null when we don't want to include it in JSON serialization.
	 */
	protected Map<String, PropertyMapping> properties;

	@SerializeExtraProperties
	private Map<String, JsonElement> extraAttributes;

	public DynamicType getDynamic() {
		return dynamic;
	}

	public void setDynamic(DynamicType dynamic) {
		this.dynamic = dynamic;
	}

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
		getInitializedProperties().put( name, mapping );
	}

	public void removeProperty(String name) {
		getInitializedProperties().remove( name );
	}

	public Map<String, JsonElement> getExtraAttributes() {
		return extraAttributes;
	}

	public void setExtraAttributes(Map<String, JsonElement> extraAttributes) {
		this.extraAttributes = extraAttributes;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}
}
