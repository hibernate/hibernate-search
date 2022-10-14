/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype;

/**
 * Abstract base for tests of automatic indexing caused by association updates
 * or by updates of associated (contained) entities,
 * with a single-valued association.
 */
public abstract class AbstractAutomaticIndexingSingleValuedAssociationBaseIT<
				TIndexed extends TContaining, TContaining, TContainingEmbeddable, TContained, TContainedEmbeddable
		>
		extends AbstractAutomaticIndexingAssociationBaseIT<
						TIndexed, TContaining, TContainingEmbeddable, TContained, TContainedEmbeddable
				> {

	public AbstractAutomaticIndexingSingleValuedAssociationBaseIT(IndexedEntityPrimitives<TIndexed> indexedPrimitives,
			ContainingEntityPrimitives<TContaining, TContainingEmbeddable, TContained> containingPrimitives,
			ContainingEmbeddablePrimitives<TContainingEmbeddable, TContained> containingEmbeddablePrimitives,
			ContainedEntityPrimitives<TContained, TContainedEmbeddable, TContaining> containedPrimitives,
			ContainedEmbeddablePrimitives<TContainedEmbeddable, TContaining> containedEmbeddablePrimitives) {
		super( indexedPrimitives, containingPrimitives, containingEmbeddablePrimitives, containedPrimitives, containedEmbeddablePrimitives );
	}

	@Override
	protected final boolean isAssociationMultiValuedOnContainingSide() {
		return false;
	}

}
