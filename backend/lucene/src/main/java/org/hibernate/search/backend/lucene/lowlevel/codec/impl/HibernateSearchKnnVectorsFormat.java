/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.codec.impl;

import static org.apache.lucene.util.hnsw.HnswGraphBuilder.DEFAULT_BEAM_WIDTH;
import static org.apache.lucene.util.hnsw.HnswGraphBuilder.DEFAULT_MAX_CONN;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.KnnVectorsReader;
import org.apache.lucene.codecs.KnnVectorsWriter;
import org.apache.lucene.codecs.lucene99.Lucene99HnswVectorsFormat;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;

public class HibernateSearchKnnVectorsFormat extends KnnVectorsFormat {
	public static final int DEFAULT_MAX_DIMENSIONS = KnnVectorsFormat.DEFAULT_MAX_DIMENSIONS;
	private static final KnnVectorsFormat DEFAULT_KNN_VECTORS_FORMAT = new HibernateSearchKnnVectorsFormat();

	public static KnnVectorsFormat defaultFormat() {
		return DEFAULT_KNN_VECTORS_FORMAT;
	}

	private final KnnVectorsFormat delegate;
	private final int m;

	private final int efConstruction;

	public HibernateSearchKnnVectorsFormat() {
		this( DEFAULT_MAX_CONN, DEFAULT_BEAM_WIDTH );
	}

	public HibernateSearchKnnVectorsFormat(int m, int efConstruction) {
		this( new Lucene99HnswVectorsFormat( m, efConstruction ), m, efConstruction );
	}

	public HibernateSearchKnnVectorsFormat(KnnVectorsFormat delegate, int m, int efConstruction) {
		super( delegate.getName() );
		this.delegate = delegate;
		this.m = m;
		this.efConstruction = efConstruction;
	}

	@Override
	public KnnVectorsWriter fieldsWriter(SegmentWriteState state) throws IOException {
		return delegate.fieldsWriter( state );
	}

	@Override
	public KnnVectorsReader fieldsReader(SegmentReadState state) throws IOException {
		return delegate.fieldsReader( state );
	}

	@Override
	public int getMaxDimensions(String fieldName) {
		// TODO: HSEARCH-5020: we can make this configurable, apparently there are models that produce larger vectors than this default allows.
		return DEFAULT_MAX_DIMENSIONS;
	}

	public KnnVectorsFormat delegate() {
		return delegate;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		HibernateSearchKnnVectorsFormat that = (HibernateSearchKnnVectorsFormat) o;
		return m == that.m && efConstruction == that.efConstruction;
	}

	@Override
	public int hashCode() {
		return Objects.hash( m, efConstruction );
	}

	@Override
	public String toString() {
		return "HibernateSearchKnnVectorsFormat{" +
				"m=" + m +
				", efConstruction=" + efConstruction +
				'}';
	}
}
