/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneKnnPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneVectorFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFloatVectorCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;

import org.apache.lucene.index.VectorSimilarityFunction;

class LuceneFloatVectorFieldTypeOptionsStep
		extends AbstractLuceneVectorFieldTypeOptionsStep<LuceneFloatVectorFieldTypeOptionsStep, float[]> {

	LuceneFloatVectorFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, float[].class );
	}

	@Override
	protected LuceneFloatVectorFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneValueFieldSearchQueryElementFactory<KnnPredicateBuilder, float[]> knnPredicateFactory() {
		return new LuceneKnnPredicate.FloatFactory();
	}

	@Override
	protected AbstractLuceneVectorFieldCodec<float[]> createCodec(VectorSimilarityFunction vectorSimilarity, int dimension,
			Storage storage, Indexing indexing, float[] indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		return new LuceneFloatVectorCodec( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat );
	}

}
