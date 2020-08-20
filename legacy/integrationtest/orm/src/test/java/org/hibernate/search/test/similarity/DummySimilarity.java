/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.similarity;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.util.BytesRef;

/**
 * @author Emmanuel Bernard
 */
public class DummySimilarity extends ClassicSimilarity {

	private float CONST = 1.0f;

	@Override
	public float queryNorm(float sumOfSquaredWeights) {
		return CONST;
	}

	@Override
	public float sloppyFreq(int distance) {
		return CONST;
	}

	@Override
	public float tf(float freq) {
		return CONST;
	}

	@Override
	public float coord(int overlap, int maxOverlap) {
		return CONST;
	}

	@Override
	public float lengthNorm(FieldInvertState state) {
		return CONST;
	}

	@Override
	public float scorePayload(int doc, int start, int end, BytesRef payload) {
		return CONST;
	}

	@Override
	public float idf(long docFreq, long numDocs) {
		return CONST;
	}

}
