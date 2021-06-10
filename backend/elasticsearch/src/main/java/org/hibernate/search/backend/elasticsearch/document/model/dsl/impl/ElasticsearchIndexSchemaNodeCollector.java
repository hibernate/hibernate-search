/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;


import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaValueFieldTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;

public interface ElasticsearchIndexSchemaNodeCollector {

	void collect(String absolutePath, ElasticsearchIndexObjectField node);

	void collect(String absoluteFieldPath, ElasticsearchIndexValueField<?> node);

	void collect(ElasticsearchIndexSchemaObjectFieldTemplate template);

	void collect(ElasticsearchIndexSchemaValueFieldTemplate template);

	void collect(NamedDynamicTemplate templateForMapping);

}
