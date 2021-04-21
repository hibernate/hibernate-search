/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexSchemaElementContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractLuceneIndexSchemaFieldNode implements IndexFieldDescriptor,
		LuceneSearchIndexSchemaElementContext {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final LuceneIndexSchemaObjectNode parent;
	protected final String absolutePath;
	protected final String relativeName;
	protected final IndexFieldInclusion inclusion;
	protected final boolean multiValued;
	protected final boolean multiValuedInRoot;
	protected final boolean dynamic;

	public AbstractLuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String relativeName,
			IndexFieldInclusion inclusion, boolean multiValued, boolean dynamic) {
		this.parent = parent;
		this.absolutePath = parent.absolutePath( relativeName );
		this.relativeName = relativeName;
		this.inclusion = parent.inclusion().compose( inclusion );
		this.multiValued = multiValued;
		this.multiValuedInRoot = multiValued || parent.multiValuedInRoot();
		this.dynamic = dynamic;
	}

	@Override
	public abstract LuceneIndexSchemaObjectFieldNode toObjectField();

	@Override
	public abstract LuceneIndexSchemaValueFieldNode<?> toValueField();

	@Override
	public LuceneIndexSchemaObjectNode parent() {
		return parent;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public String relativeName() {
		return relativeName;
	}

	public IndexFieldInclusion inclusion() {
		return inclusion;
	}

	@Override
	public boolean multiValued() {
		return multiValued;
	}

	@Override
	public boolean multiValuedInRoot() {
		return multiValuedInRoot;
	}

	@Override
	public EventContext eventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

	public boolean dynamic() {
		return dynamic;
	}
}
