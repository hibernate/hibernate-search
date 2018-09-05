/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Collection;

import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.engine.metadata.impl.PartialDocumentFieldMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;

/**
 * An object responsible for translating a Hibernate Search schema to an Elasticsearch schema.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchSchemaTranslator {

	/**
	 * Translates index metadata, throwing an exception if translation fails.
	 *
	 * @param indexName The name of the index
	 * @param descriptors The entity bindings
	 * @param executionOptions The execution options, giving more context information.
	 * @throws SearchException If an error occurs.
	 */
	IndexMetadata translate(URLEncodedString indexName, Collection<EntityIndexBinding> descriptors, ExecutionOptions executionOptions);

	/**
	 * @param fieldMetadata Metadata to inspect
	 * @return {@code true} if the given field will have the "text" datatype on Elasticsearch, {@code false} otherwise.
	 */
	boolean isTextDataType(PartialDocumentFieldMetadata fieldMetadata);

}
