/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoBoundingBox;


public abstract class AbstractLuceneSpatialWithinBoundingBoxPredicateBuilder
		extends AbstractLuceneSingleFieldPredicateBuilder
		implements SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> {

	protected GeoBoundingBox boundingBox;

	protected AbstractLuceneSpatialWithinBoundingBoxPredicateBuilder(LuceneSearchFieldContext<?> field) {
		super( field );
	}

	@Override
	public void boundingBox(GeoBoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}
}
