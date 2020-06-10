/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopedIndexRootComponent;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;

/**
 * Information about indexes targeted by search,
 * be it in a projection, a predicate, a sort, ...
 */
public interface ElasticsearchSearchIndexesContext {

	Set<String> mappedTypeNames();

	Set<String> hibernateSearchIndexNames();

	Collection<URLEncodedString> elasticsearchIndexNames();

	Map<String, URLEncodedString> mappedTypeToElasticsearchIndexNames();

	ElasticsearchScopedIndexRootComponent<ToDocumentIdentifierValueConverter<?>> idDslConverter();

	ElasticsearchSearchFieldContext<?> field(String absoluteFieldPath);

	boolean hasSchemaObjectNodeComponent(String absoluteFieldPath);

	void checkNestedField(String absoluteFieldPath);

	List<String> nestedPathHierarchyForObject(String absoluteObjectPath);
}
