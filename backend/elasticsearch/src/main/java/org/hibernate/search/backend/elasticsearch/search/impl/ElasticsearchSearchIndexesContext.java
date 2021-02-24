/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNamedPredicateNode;

import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.ValueConvert;

/**
 * Information about indexes targeted by search,
 * be it in a projection, a predicate, a sort, ...
 */
public interface ElasticsearchSearchIndexesContext {

	Collection<ElasticsearchSearchIndexContext> elements();

	Set<String> hibernateSearchIndexNames();

	Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex();

	ToDocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert);

	ElasticsearchSearchFieldContext field(String absoluteFieldPath);

	ElasticsearchIndexSchemaNamedPredicateNode namedPredicate(String absoluteNamedPredicatePath);
}