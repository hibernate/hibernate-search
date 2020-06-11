/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopedIndexRootComponent;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectPredicateBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;

public interface LuceneSearchIndexesContext {

	Set<String> typeNames();

	Set<String> indexNames();

	Set<? extends ReadIndexManagerContext> indexManagerContexts();

	LuceneScopedIndexRootComponent<ToDocumentIdentifierValueConverter<?>> idDslConverter();

	LuceneObjectPredicateBuilderFactory objectPredicateBuilderFactory(String absoluteFieldPath);

	LuceneSearchFieldContext<?> field(String absoluteFieldPath);

	void checkNestedField(String absoluteFieldPath);

	List<String> nestedPathHierarchyForObject(String absoluteFieldPath);

}
