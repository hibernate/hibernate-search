/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexCompositeElementDescriptor;


public interface ElasticsearchIndexSchemaObjectNode extends IndexCompositeElementDescriptor {

	String absolutePath();

	String absolutePath(String relativeFieldName);

	IndexFieldInclusion inclusion();

	List<String> nestedPathHierarchy();

	@Override
	default Collection<? extends AbstractElasticsearchIndexSchemaFieldNode> staticChildren() {
		return staticChildrenByName().values();
	}

	@Override
	Map<String, ? extends AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByName();

	boolean multiValuedInRoot();

}
