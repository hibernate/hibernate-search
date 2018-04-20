/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchIndexSchemaElement extends IndexSchemaElement {

	@Override
	default ElasticsearchIndexSchemaObjectField objectField(String relativeFieldName) {
		return objectField( relativeFieldName, ObjectFieldStorage.DEFAULT );
	}

	@Override
	ElasticsearchIndexSchemaObjectField objectField(String relativeFieldName, ObjectFieldStorage storage);
}
