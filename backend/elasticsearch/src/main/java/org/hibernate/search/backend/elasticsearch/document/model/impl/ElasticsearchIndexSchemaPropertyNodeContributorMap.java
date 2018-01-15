/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.TypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.util.SearchException;

/**
 * A map of name to property node contributors.
 * <p>
 * Allows sharing of contributed properties across multiple
 * {@link org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaElement},
 * in order to avoid conflicting contributions.
 */
class ElasticsearchIndexSchemaPropertyNodeContributorMap {

	private final JsonObjectAccessor accessor;
	private final Map<String, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> content = new HashMap<>();

	public ElasticsearchIndexSchemaPropertyNodeContributorMap(JsonObjectAccessor accessor) {
		this.accessor = accessor;
	}

	public void put(String name, ElasticsearchIndexSchemaNodeContributor<PropertyMapping> contributor) {
		Object previous = content.putIfAbsent( name, contributor );
		if ( previous != null ) {
			// TODO more explicit error message
			throw new SearchException( "The index model node '" + name + "' was added twice at path '" + accessor + "'."
					+ " Multiple bridges may be trying to access the same index field, "
					+ " or two indexedEmbeddeds may have prefixes that end up mixing fields together,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it." );
		}
	}

	public void contribute(ElasticsearchFieldModelCollector collector, TypeMapping mapping) {
		for ( Map.Entry<String, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> entry : content.entrySet() ) {
			String propertyName = entry.getKey();
			ElasticsearchIndexSchemaNodeContributor<PropertyMapping> propertyContributor = entry.getValue();
			PropertyMapping propertyMapping = propertyContributor.contribute( collector );
			mapping.addProperty( propertyName, propertyMapping );
		}
	}

}
