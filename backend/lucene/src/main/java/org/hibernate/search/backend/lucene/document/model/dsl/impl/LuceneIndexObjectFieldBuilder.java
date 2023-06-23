/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexObjectFieldReference;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectField;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexObjectFieldBuilder;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

class LuceneIndexObjectFieldBuilder extends AbstractLuceneIndexCompositeNodeBuilder
		implements IndexObjectFieldBuilder, LuceneIndexNodeContributor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractLuceneIndexCompositeNodeBuilder parent;
	private final String absoluteFieldPath;
	private final String relativeFieldName;
	private final TreeNodeInclusion inclusion;
	private boolean multiValued = false;

	private LuceneIndexObjectFieldReference reference;

	LuceneIndexObjectFieldBuilder(AbstractLuceneIndexCompositeNodeBuilder parent,
			String relativeFieldName, TreeNodeInclusion inclusion, ObjectStructure structure) {
		super( new LuceneIndexCompositeNodeType.Builder( structure ) );
		this.parent = parent;
		this.absoluteFieldPath = FieldPaths.compose( parent.getAbsolutePath(), relativeFieldName );
		this.relativeFieldName = relativeFieldName;
		this.inclusion = inclusion;
	}

	@Override
	public EventContext eventContext() {
		return getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public void multiValued() {
		this.multiValued = true;
	}

	@Override
	public IndexObjectFieldReference toReference() {
		if ( reference != null ) {
			throw log.cannotCreateReferenceMultipleTimes( eventContext() );
		}
		this.reference = new LuceneIndexObjectFieldReference();
		return reference;
	}

	@Override
	public void contribute(LuceneIndexNodeCollector collector, LuceneIndexCompositeNode parentNode,
			Map<String, LuceneIndexField> staticChildrenByNameForParent) {
		if ( reference == null ) {
			throw log.incompleteFieldDefinition( eventContext() );
		}

		Map<String, LuceneIndexField> staticChildrenByName = new TreeMap<>();
		LuceneIndexObjectField node = new LuceneIndexObjectField(
				parentNode, relativeFieldName, typeBuilder.build(), inclusion, multiValued,
				staticChildrenByName, false
		);

		staticChildrenByNameForParent.put( relativeFieldName, node );
		collector.collect( absoluteFieldPath, node );

		reference.setSchemaNode( node );

		contributeChildren( node, collector, staticChildrenByName );
	}

	@Override
	public LuceneIndexRootBuilder getRootNodeBuilder() {
		return parent.getRootNodeBuilder();
	}

	@Override
	String getAbsolutePath() {
		return absoluteFieldPath;
	}
}
