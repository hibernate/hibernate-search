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
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFilterReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFilterNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.MetadataFields;
import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFilterOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchIndexSchemaFilterFactoryBuilder<F extends FilterFactory> implements IndexSchemaFilterOptionsStep<ElasticsearchIndexSchemaFilterFactoryBuilder<F>, IndexFilterReference<F>>,
	ElasticsearchIndexSchemaNodeContributor, IndexSchemaBuildContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractElasticsearchIndexSchemaObjectNodeBuilder parent;
	private final String relativeFilterName;
	private final String absoluteFilterPath;
	private final Map<String, Object> params = new LinkedHashMap<>();
	private final F factory;

	private ElasticsearchIndexFilterReference<F> reference;

	ElasticsearchIndexSchemaFilterFactoryBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent, String relativeFilterName, F factory) {
		this.parent = parent;
		this.relativeFilterName = relativeFilterName;
		this.absoluteFilterPath = MetadataFields.compose( parent.getAbsolutePath(), relativeFilterName );
		this.factory = factory;
	}

	@Override
	public <T> ElasticsearchIndexSchemaFilterFactoryBuilder param(String name, T value) {
		this.params.put( name, value );
		return this;
	}

	@Override
	public ElasticsearchIndexSchemaFilterFactoryBuilder<F> params(Map<String, Object> params) {
		this.params.putAll( params );
		return this;
	}

	@Override
	public void contribute(ElasticsearchIndexSchemaNodeCollector collector, ElasticsearchIndexSchemaObjectNode parentNode,
		AbstractTypeMapping parentMapping) {
		if ( reference == null ) {
			throw log.incompleteFilterDefinition( getEventContext() );
		}

		ElasticsearchIndexSchemaFilterNode<F> filterNode = new ElasticsearchIndexSchemaFilterNode<>(
			parentNode, relativeFilterName, factory, params
		);

		reference.enable( filterNode );

		collector.collect( absoluteFilterPath, filterNode );
	}

	@Override
	public EventContext getEventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
			.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFilterPath ) );
	}

	@Override
	public IndexFilterReference<F> toReference() {
		if ( reference != null ) {
			throw log.cannotCreateFilterReferenceMultipleTimes( getEventContext() );
		}
		this.reference = new ElasticsearchIndexFilterReference<>( absoluteFilterPath, factory );
		return reference;
	}
}
