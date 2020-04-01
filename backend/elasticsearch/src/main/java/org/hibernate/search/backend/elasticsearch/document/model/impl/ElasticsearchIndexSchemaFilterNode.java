/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchIndexSchemaFilterNode<F extends FilterFactory> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchIndexSchemaObjectNode parent;
	private final String parentDocumentPath;
	private final String relativeFilterName;
	private final String absoluteFilterPath;
	private final List<String> nestedPathHierarchy;

	private final F factory;
	private final Map<String, Object> params;

	public ElasticsearchIndexSchemaFilterNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFilterName,
		F factory, Map<String, Object> params) {
		this.parent = parent;
		this.relativeFilterName = relativeFilterName;
		this.parentDocumentPath = parent.getAbsolutePath();
		this.absoluteFilterPath = parent.getAbsolutePath( relativeFilterName );
		this.nestedPathHierarchy = parent.getNestedPathHierarchy();
		this.factory = factory;
		this.params = params;
	}

	public ElasticsearchIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getParentDocumentPath() {
		return parentDocumentPath;
	}

	public String getNestedDocumentPath() {
		return (nestedPathHierarchy.isEmpty()) ? null
			: nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	public String getAbsoluteFilterPath() {
		return absoluteFilterPath;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public F getFactory() {
		return factory;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
			.append( "parent=" ).append( parent )
			.append( ", relativeFieldName=" ).append( relativeFilterName )
			.append( "]" );
		return sb.toString();
	}

	private EventContext getEventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absoluteFilterPath );
	}
}
