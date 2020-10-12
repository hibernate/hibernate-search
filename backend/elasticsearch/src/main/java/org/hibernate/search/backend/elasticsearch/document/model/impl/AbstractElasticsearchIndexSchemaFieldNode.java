/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonElement;

public abstract class AbstractElasticsearchIndexSchemaFieldNode
		implements IndexFieldDescriptor, ElasticsearchSearchFieldContext {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final ElasticsearchIndexSchemaObjectNode parent;
	protected final String absolutePath;
	protected final String[] absolutePathComponents;
	protected final String relativeName;
	protected final JsonAccessor<JsonElement> relativeAccessor;

	protected final IndexFieldInclusion inclusion;
	protected final boolean multiValued;
	protected final boolean multiValuedInRoot;

	public AbstractElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent,
			String relativeFieldName,
			IndexFieldInclusion inclusion, boolean multiValued) {
		this.parent = parent;
		this.absolutePath = parent.absolutePath( relativeFieldName );
		this.absolutePathComponents = FieldPaths.split( absolutePath );
		this.relativeName = relativeFieldName;
		this.relativeAccessor = JsonAccessor.root().property( relativeFieldName );
		this.inclusion = inclusion;
		this.multiValued = multiValued;
		this.multiValuedInRoot = multiValued || parent.multiValuedInRoot();
	}

	@Override
	public abstract ElasticsearchIndexSchemaObjectFieldNode toObjectField();

	@Override
	public abstract ElasticsearchIndexSchemaValueFieldNode<?> toValueField();

	@Override
	public ElasticsearchIndexSchemaObjectNode parent() {
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

	public JsonAccessor<JsonElement> relativeAccessor() {
		return relativeAccessor;
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
