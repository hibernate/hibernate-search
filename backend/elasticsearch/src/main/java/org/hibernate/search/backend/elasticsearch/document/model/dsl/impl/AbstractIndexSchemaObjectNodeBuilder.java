/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexObjectFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A schema node builder.
 */
abstract class AbstractIndexSchemaObjectNodeBuilder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final DeferredInitializationIndexObjectFieldAccessor accessor =
			new DeferredInitializationIndexObjectFieldAccessor();

	private final Map<String, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> content = new HashMap<>();

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( ",absolutePath=" ).append( getAbsolutePath() )
				.append( "]" )
				.toString();
	}

	public IndexObjectFieldAccessor getAccessor() {
		return accessor;
	}

	public abstract String getAbsolutePath();

	public void putProperty(String name, ElasticsearchIndexSchemaNodeContributor<PropertyMapping> contributor) {
		Object previous = content.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNodeNameConflict( getAbsolutePath(), name);
		}
	}

	final void contributeChildren(AbstractTypeMapping mapping, ElasticsearchIndexSchemaObjectNode node,
			ElasticsearchIndexSchemaNodeCollector collector) {
		for ( Map.Entry<String, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> entry : content.entrySet() ) {
			String propertyName = entry.getKey();
			ElasticsearchIndexSchemaNodeContributor<PropertyMapping> propertyContributor = entry.getValue();
			PropertyMapping propertyMapping = propertyContributor.contribute( collector, node );
			mapping.addProperty( propertyName, propertyMapping );
		}
	}

}
