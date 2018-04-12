/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl.esnative;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

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
@JsonAdapter(RootTypeMappingJsonAdapterFactory.class)
public class RootTypeMapping extends AbstractTypeMapping {

	@SerializedName( "_routing" )
	private RoutingType routing;

	public RoutingType getRouting() {
		return routing;
	}

	public void setRouting(RoutingType routing) {
		this.routing = routing;
	}
}
