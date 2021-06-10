/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexNodeContext;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractElasticsearchIndexField
		implements IndexFieldDescriptor, ElasticsearchSearchIndexNodeContext {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final ElasticsearchIndexCompositeNode parent;
	protected final String absolutePath;
	protected final String[] absolutePathComponents;
	protected final String relativeName;

	protected final IndexFieldInclusion inclusion;
	protected final boolean multiValued;
	protected final boolean multiValuedInRoot;

	public AbstractElasticsearchIndexField(ElasticsearchIndexCompositeNode parent,
			String relativeFieldName,
			IndexFieldInclusion inclusion, boolean multiValued) {
		this.parent = parent;
		this.absolutePath = parent.absolutePath( relativeFieldName );
		this.absolutePathComponents = FieldPaths.split( absolutePath );
		this.relativeName = relativeFieldName;
		this.inclusion = inclusion;
		this.multiValued = multiValued;
		this.multiValuedInRoot = multiValued || parent.multiValuedInRoot();
	}

	@Override
	public abstract ElasticsearchIndexObjectField toObjectField();

	@Override
	public abstract ElasticsearchIndexValueField<?> toValueField();

	@Override
	public ElasticsearchIndexCompositeNode parent() {
		return parent;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	public String[] absolutePathComponents() {
		return absolutePathComponents;
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

}
