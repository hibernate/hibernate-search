/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.SearchException;

public final class LuceneMultiIndexSearchCompositeIndexSchemaElementContext
		extends AbstractLuceneMultiIndexSearchIndexSchemaElementContext<LuceneSearchCompositeIndexSchemaElementContext>
		implements LuceneSearchCompositeIndexSchemaElementContext {

	private Map<String, LuceneSearchIndexSchemaElementContext> staticChildrenByName;

	public LuceneMultiIndexSearchCompositeIndexSchemaElementContext(LuceneSearchIndexScope scope,
			String absolutePath, List<LuceneSearchCompositeIndexSchemaElementContext> elementForEachIndex) {
		super( scope, absolutePath, elementForEachIndex );
	}

	@Override
	protected LuceneSearchCompositeIndexSchemaElementContext self() {
		return this;
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	public LuceneSearchCompositeIndexSchemaElementContext toComposite() {
		return this;
	}

	@Override
	public String absolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath, relativeFieldName );
	}

	@Override
	public Map<String, ? extends LuceneSearchIndexSchemaElementContext> staticChildrenByName() {
		if ( staticChildrenByName != null ) {
			return staticChildrenByName;
		}

		// TODO HSEARCH-4050 remove this unnecessary restriction?
		getFromElementIfCompatible( field -> field.staticChildrenByName().keySet(),
				Object::equals, "staticChildren" );

		Map<String, LuceneSearchIndexSchemaElementContext> result = new TreeMap<>();
		Function<String, LuceneSearchIndexSchemaElementContext> createChildFieldContext = scope::field;
		for ( LuceneSearchCompositeIndexSchemaElementContext indexElement : elementForEachIndex ) {
			for ( LuceneSearchIndexSchemaElementContext child : indexElement.staticChildrenByName().values() ) {
				try {
					result.computeIfAbsent( child.absolutePath(), createChildFieldContext );
				}
				catch (SearchException e) {
					throw log.inconsistentConfigurationForIndexElementForSearch( relativeEventContext(), e.getMessage(),
							indexesEventContext(), e );
				}
			}
		}
		// Only set this to a non-null value if we didn't detect any conflict during the loop.
		// If there was a conflict, we want the next call to this method to go through the loop again
		// and throw an exception again.
		staticChildrenByName = result;
		return staticChildrenByName;
	}

	@Override
	public boolean nested() {
		return getFromElementIfCompatible( LuceneSearchCompositeIndexSchemaElementContext::nested, Object::equals,
				"nested" );
	}

	@Override
	protected String missingSupportHint(String queryElementName) {
		return log.missingSupportHintForCompositeIndexElement();
	}

	@Override
	protected String partialSupportHint() {
		return log.partialSupportHintForCompositeIndexElement();
	}

	@Override
	protected <T> LuceneSearchQueryElementFactory<T, LuceneSearchCompositeIndexSchemaElementContext> queryElementFactory(
			LuceneSearchCompositeIndexSchemaElementContext indexElement, SearchQueryElementTypeKey<T> key) {
		return indexElement.queryElementFactory( key );
	}

}
