/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFilterReference;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFilterNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFilterOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneIndexSchemaFilterFactoryBuilder<F extends FilterFactory> implements IndexSchemaFilterOptionsStep<LuceneIndexSchemaFilterFactoryBuilder<F>, IndexFilterReference<F>>,
	LuceneIndexSchemaNodeContributor, IndexSchemaBuildContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractLuceneIndexSchemaObjectNodeBuilder parent;
	private final String relativeFilterName;
	private final String absoluteFilterPath;
	private final Map<String, Object> params = new LinkedHashMap<>();
	private final F factory;

	private LuceneIndexFilterReference<F> reference;

	LuceneIndexSchemaFilterFactoryBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent, String relativeFilterName, F factory) {
		this.parent = parent;
		this.relativeFilterName = relativeFilterName;
		this.absoluteFilterPath = MetadataFields.compose( parent.getAbsolutePath(), relativeFilterName );
		this.factory = factory;
	}

	@Override
	public <T> LuceneIndexSchemaFilterFactoryBuilder param(String name, T value) {
		this.params.put( name, value );
		return this;
	}

	@Override
	public LuceneIndexSchemaFilterFactoryBuilder<F> params(Map<String, Object> params) {
		this.params.putAll( params );
		return this;
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		if ( reference == null ) {
			throw log.incompleteFilterDefinition( getEventContext() );
		}

		LuceneIndexSchemaFilterNode<F> filterNode = new LuceneIndexSchemaFilterNode<>(
			parentNode, relativeFilterName, factory, params
		);

		reference.enable( filterNode );

		collector.collectFilterNode( absoluteFilterPath, filterNode );
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
		this.reference = new LuceneIndexFilterReference<>( absoluteFilterPath, factory );
		return reference;
	}
}
