/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeTypeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneNestedPredicate;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneObjectProjection;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectExistsPredicate;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexCompositeNodeType;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

public class LuceneIndexCompositeNodeType
		extends AbstractIndexCompositeNodeType<
						LuceneSearchIndexScope<?>, LuceneSearchIndexCompositeNodeContext
				>
		implements LuceneSearchIndexCompositeNodeTypeContext {

	private LuceneIndexCompositeNodeType(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends AbstractIndexCompositeNodeType.Builder<
							LuceneSearchIndexScope<?>, LuceneSearchIndexCompositeNodeContext
					> {
		public Builder(ObjectStructure objectStructure) {
			super( objectStructure );
			queryElementFactory( PredicateTypeKeys.EXISTS, LuceneObjectExistsPredicate.Factory.INSTANCE );
			queryElementFactory( ProjectionTypeKeys.OBJECT, new LuceneObjectProjection.Factory() );
			if ( ObjectStructure.NESTED.equals( objectStructure ) ) {
				queryElementFactory( PredicateTypeKeys.NESTED, LuceneNestedPredicate.Factory.INSTANCE );
			}
		}

		@Override
		public LuceneIndexCompositeNodeType build() {
			return new LuceneIndexCompositeNodeType( this );
		}
	}
}
