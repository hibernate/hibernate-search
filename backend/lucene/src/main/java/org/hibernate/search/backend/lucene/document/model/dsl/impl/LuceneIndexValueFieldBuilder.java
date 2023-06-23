/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldReference;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueField;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

class LuceneIndexValueFieldBuilder<F>
		implements IndexSchemaFieldOptionsStep<LuceneIndexValueFieldBuilder<F>, IndexFieldReference<F>>,
		LuceneIndexNodeContributor, IndexSchemaBuildContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractLuceneIndexCompositeNodeBuilder parent;
	private final String relativeFieldName;
	private final String absoluteFieldPath;
	private final TreeNodeInclusion inclusion;
	private final LuceneIndexValueFieldType<F> type;
	private boolean multiValued = false;

	private LuceneIndexFieldReference<F> reference;

	LuceneIndexValueFieldBuilder(AbstractLuceneIndexCompositeNodeBuilder parent,
			String relativeFieldName, TreeNodeInclusion inclusion, LuceneIndexValueFieldType<F> type) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = FieldPaths.compose( parent.getAbsolutePath(), relativeFieldName );
		this.inclusion = inclusion;
		this.type = type;
	}

	@Override
	public EventContext eventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public LuceneIndexValueFieldBuilder<F> multiValued() {
		this.multiValued = true;
		return this;
	}

	@Override
	public IndexFieldReference<F> toReference() {
		if ( reference != null ) {
			throw log.cannotCreateReferenceMultipleTimes( eventContext() );
		}
		this.reference = new LuceneIndexFieldReference<>();
		return reference;
	}

	@Override
	public void contribute(LuceneIndexNodeCollector collector, LuceneIndexCompositeNode parentNode,
			Map<String, LuceneIndexField> staticChildrenByNameForParent) {
		if ( reference == null ) {
			throw log.incompleteFieldDefinition( eventContext() );
		}
		LuceneIndexValueField<F> fieldNode = new LuceneIndexValueField<>( parentNode, relativeFieldName, type,
				inclusion, multiValued, false );

		staticChildrenByNameForParent.put( relativeFieldName, fieldNode );
		collector.collect( fieldNode.absolutePath(), fieldNode );

		reference.setSchemaNode( fieldNode );
	}

}
