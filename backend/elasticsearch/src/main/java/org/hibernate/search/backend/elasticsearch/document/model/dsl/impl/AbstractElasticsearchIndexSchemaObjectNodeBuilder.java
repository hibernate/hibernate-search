/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

abstract class AbstractElasticsearchIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> content = new LinkedHashMap<>();

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absolutePath=" ).append( getAbsolutePath() )
				.append( "]" )
				.toString();
	}

	@Override
	public IndexSchemaFieldContext addField(String relativeFieldName) {
		ElasticsearchIndexSchemaFieldContextImpl fieldContext =
				new ElasticsearchIndexSchemaFieldContextImpl( relativeFieldName );
		putProperty( relativeFieldName, fieldContext );
		return fieldContext;
	}

	@Override
	public IndexSchemaFieldContext createExcludedField(String relativeFieldName) {
		return new ElasticsearchIndexSchemaFieldContextImpl( relativeFieldName );
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		ElasticsearchIndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
				new ElasticsearchIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, storage );
		putProperty( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder createExcludedObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		return new ElasticsearchIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, storage );
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

	abstract ElasticsearchIndexSchemaRootNodeBuilder getRootNodeBuilder();

	abstract String getAbsolutePath();

	private void putProperty(String name, ElasticsearchIndexSchemaNodeContributor<PropertyMapping> contributor) {
		Object previous = content.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNodeNameConflict( name, getFailureContext() );
		}
	}

}
