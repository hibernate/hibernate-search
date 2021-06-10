/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.search.common.spi.AbstractMultiIndexSearchIndexCompositeNodeContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class LuceneMultiIndexSearchIndexCompositeNodeContext
		extends AbstractMultiIndexSearchIndexCompositeNodeContext<
						LuceneSearchIndexCompositeNodeContext,
						LuceneSearchIndexScope,
						LuceneSearchIndexCompositeNodeTypeContext
				>
		implements LuceneSearchIndexCompositeNodeContext, LuceneSearchIndexCompositeNodeTypeContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private Map<String, LuceneSearchIndexNodeContext> staticChildrenByName;

	public LuceneMultiIndexSearchIndexCompositeNodeContext(LuceneSearchIndexScope scope,
			String absolutePath, List<? extends LuceneSearchIndexCompositeNodeContext> nodeForEachIndex) {
		super( scope, absolutePath, nodeForEachIndex );
	}

	@Override
	protected LuceneSearchIndexCompositeNodeContext self() {
		return this;
	}

	@Override
	protected LuceneSearchIndexCompositeNodeTypeContext selfAsNodeType() {
		return this;
	}

	@Override
	protected LuceneSearchIndexCompositeNodeTypeContext typeOf(LuceneSearchIndexCompositeNodeContext indexElement) {
		return indexElement.type();
	}

	@Override
	public Map<String, ? extends LuceneSearchIndexNodeContext> staticChildrenByName() {
		if ( staticChildrenByName != null ) {
			return staticChildrenByName;
		}

		// TODO HSEARCH-4050 remove this unnecessary restriction?
		fromNodeIfCompatible( field -> field.staticChildrenByName().keySet(),
				Object::equals, "staticChildren" );

		Map<String, LuceneSearchIndexNodeContext> result = new TreeMap<>();
		Function<String, LuceneSearchIndexNodeContext> createChildFieldContext = scope::field;
		for ( LuceneSearchIndexCompositeNodeContext indexElement : nodeForEachIndex ) {
			for ( LuceneSearchIndexNodeContext child : indexElement.staticChildrenByName().values() ) {
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

}
