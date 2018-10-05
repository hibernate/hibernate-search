/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;


/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchIndexSchemaFieldContext extends IndexSchemaFieldContext {

	IndexSchemaFieldTypedContext<?, String> asJsonString(String mappingJsonString);

}
