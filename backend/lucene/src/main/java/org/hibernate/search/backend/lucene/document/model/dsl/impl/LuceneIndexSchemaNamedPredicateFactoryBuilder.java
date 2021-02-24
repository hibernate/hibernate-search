/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.util.Map;
import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNamedPredicateNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateFactory;

public class LuceneIndexSchemaNamedPredicateFactoryBuilder implements IndexSchemaNamedPredicateOptionsStep,
		LuceneIndexSchemaNodeContributor, IndexSchemaBuildContext {

	private final AbstractLuceneIndexSchemaObjectNodeBuilder parent;
	private final String relativeNamedPredicateName;
	private final String absoluteNamedPredicatePath;
	private final NamedPredicateFactory factory;

	LuceneIndexSchemaNamedPredicateFactoryBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent, String relativeNamedPredicateName, NamedPredicateFactory factory) {
		this.parent = parent;
		this.relativeNamedPredicateName = relativeNamedPredicateName;
		this.absoluteNamedPredicatePath = FieldPaths.compose( parent.getAbsolutePath(), relativeNamedPredicateName );
		this.factory = factory;
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode,
			Map<String, AbstractLuceneIndexSchemaFieldNode> staticChildrenByNameForParent) {

		LuceneIndexSchemaNamedPredicateNode namedPredicateNode = new LuceneIndexSchemaNamedPredicateNode(
				parentNode, relativeNamedPredicateName, factory
		);

		collector.collect( absoluteNamedPredicatePath, namedPredicateNode );
	}

	@Override
	public EventContext eventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteNamedPredicatePath ) );
	}

}
