/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * An object representing Elasticsearch type mappings.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html#mapping-type
 */
/*
 * CAUTION: JSON serialization is controlled by a specific adapter, which must be
 * updated whenever fields of this class are added, renamed or removed.
 */
@JsonAdapter(RootTypeMappingJsonAdapterFactory.class)
public class RootTypeMapping extends AbstractTypeMapping {

	@SerializedName("_routing")
	private RoutingType routing;

	@SerializedName("dynamic_templates")
	private List<NamedDynamicTemplate> dynamicTemplates;

	public RoutingType getRouting() {
		return routing;
	}

	public void setRouting(RoutingType routing) {
		this.routing = routing;
	}

	public List<NamedDynamicTemplate> getDynamicTemplates() {
		return dynamicTemplates == null ? null : Collections.unmodifiableList( dynamicTemplates );
	}

	private List<NamedDynamicTemplate> getInitializedDynamicTemplates() {
		if ( dynamicTemplates == null ) {
			dynamicTemplates = new ArrayList<>();
		}
		return dynamicTemplates;
	}

	public void addDynamicTemplate(NamedDynamicTemplate template) {
		getInitializedDynamicTemplates().add( template );
	}

}
