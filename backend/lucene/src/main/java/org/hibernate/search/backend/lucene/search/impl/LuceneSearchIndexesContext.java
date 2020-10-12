/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectPredicateBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.ValueConvert;

public interface LuceneSearchIndexesContext {

	Collection<? extends LuceneSearchIndexContext> elements();

	Map<String, ? extends LuceneSearchIndexContext> mappedTypeNameToIndex();

	Set<String> indexNames();

	ToDocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert);

	LuceneObjectPredicateBuilderFactory objectPredicateBuilderFactory(String absoluteFieldPath);

	LuceneSearchFieldContext field(String absoluteFieldPath);

	boolean hasNestedDocuments();

}
