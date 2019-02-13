/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

class LuceneIndexSchemaFieldNodeBuilder<F>
		implements IndexSchemaFieldTerminalContext<IndexFieldAccessor<F>>,
		LuceneIndexSchemaNodeContributor, IndexSchemaBuildContext {

	private final AbstractLuceneIndexSchemaObjectNodeBuilder parent;
	private final String relativeFieldName;
	private final String absoluteFieldPath;
	private final LuceneIndexFieldType<F> type;

	private final IndexSchemaFieldDefinitionHelper<F> helper;

	LuceneIndexSchemaFieldNodeBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName, LuceneIndexFieldType<F> type) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = LuceneFields.compose( parent.getAbsolutePath(), relativeFieldName );
		this.type = type;
		this.helper = new IndexSchemaFieldDefinitionHelper<>( this );
	}

	@Override
	public EventContext getEventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public IndexFieldAccessor<F> createAccessor() {
		return helper.createAccessor();
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		IndexFieldAccessor<F> accessor = type.addField( collector, parentNode, relativeFieldName );
		helper.initialize( accessor );
	}

}
