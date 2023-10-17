/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneVectorFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFloatVectorCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

class LuceneFloatVectorFieldTypeOptionsStep
		extends AbstractLuceneVectorFieldTypeOptionsStep<LuceneFloatVectorFieldTypeOptionsStep, float[]> {

	LuceneFloatVectorFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, int dimension) {
		super( buildContext, float[].class, dimension );
	}

	@Override
	protected LuceneFloatVectorFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneVectorFieldCodec<float[], ?> createCodec(VectorSimilarity vectorSimilarity, int dimension,
			Indexing indexing, Storage storage, float[] indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		return new LuceneFloatVectorCodec( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat );
	}

}
