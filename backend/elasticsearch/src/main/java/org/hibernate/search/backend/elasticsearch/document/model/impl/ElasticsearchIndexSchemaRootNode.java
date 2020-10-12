/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public class ElasticsearchIndexSchemaRootNode implements ElasticsearchIndexSchemaObjectNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByName;

	public ElasticsearchIndexSchemaRootNode(
			Map<String, AbstractElasticsearchIndexSchemaFieldNode> notYetInitializedStaticChildren) {
		// We expect the children to be added to the list externally, just after the constructor call.
		this.staticChildrenByName = Collections.unmodifiableMap( notYetInitializedStaticChildren );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public boolean isRoot() {
		return true;
	}

	@Override
	public boolean isObjectField() {
		return false;
	}

	@Override
	public IndexObjectFieldDescriptor toObjectField() {
		throw log.invalidIndexElementTypeRootIsNotObjectField();
	}

	@Override
	public String absolutePath() {
		return null;
	}

	@Override
	public String absolutePath(String relativeFieldName) {
		return relativeFieldName;
	}

	@Override
	public IndexFieldInclusion inclusion() {
		return IndexFieldInclusion.INCLUDED;
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return Collections.emptyList();
	}

	@Override
	public Map<String, ? extends AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByName() {
		return staticChildrenByName;
	}

	@Override
	public boolean multiValuedInRoot() {
		return false;
	}
}
