/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNamedPredicateNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProvider;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchIndexSchemaNamedPredicateNodeBuilder implements IndexSchemaNamedPredicateOptionsStep,
		ElasticsearchIndexSchemaNodeContributor, IndexSchemaBuildContext {

	private final AbstractElasticsearchIndexSchemaObjectNodeBuilder parent;
	private final String relativeNamedPredicateName;
	private final String absoluteNamedPredicatePath;
	private final IndexFieldInclusion inclusion;
	private final NamedPredicateProvider provider;

	ElasticsearchIndexSchemaNamedPredicateNodeBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent,
			String relativeNamedPredicateName, IndexFieldInclusion inclusion, NamedPredicateProvider provider) {
		this.parent = parent;
		this.relativeNamedPredicateName = relativeNamedPredicateName;
		this.absoluteNamedPredicatePath = FieldPaths.compose( parent.getAbsolutePath(), relativeNamedPredicateName );
		this.inclusion = inclusion;
		this.provider = provider;
	}

	@Override
	public void contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode,
			Map<String, AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByNameForParent,
			AbstractTypeMapping parentMapping) {
		if ( IndexFieldInclusion.EXCLUDED.equals( inclusion ) ) {
			return;
		}

		ElasticsearchIndexSchemaNamedPredicateNode namedPredicateNode = new ElasticsearchIndexSchemaNamedPredicateNode(
				parentNode, relativeNamedPredicateName, provider
		);

		collector.collect( absoluteNamedPredicatePath, namedPredicateNode );
	}

	@Override
	public EventContext eventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteNamedPredicatePath ) );
	}

}
