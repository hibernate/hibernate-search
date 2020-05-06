/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;


import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;

public interface ElasticsearchIndexSchemaNodeCollector {

	void collect(String absolutePath, ElasticsearchIndexSchemaObjectFieldNode node);

	void collect(String absoluteFieldPath, ElasticsearchIndexSchemaFieldNode<?> node);

	void collect(ElasticsearchIndexSchemaObjectFieldTemplate template);

	void collect(ElasticsearchIndexSchemaFieldTemplate template);

	void collect(NamedDynamicTemplate templateForMapping);

}
