/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.codec.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneVectorFieldCodec;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.lucene95.Lucene95Codec;
import org.apache.lucene.codecs.perfield.PerFieldKnnVectorsFormat;

public class HibernateSearchLuceneCodec extends FilterCodec {

	public static final Codec DEFAULT_CODEC = new Lucene95Codec();

	private final KnnVectorsFormat knnVectorsFormat;

	public HibernateSearchLuceneCodec(LuceneIndexModel model) {
		this( new IndexModelBasedPerFieldKnnVectorsFormat( model ) );
	}

	public HibernateSearchLuceneCodec(KnnVectorsFormat knnVectorsFormat) {
		// We are using the name of the default codec to trick Lucene into writing it as a codec into the segment info.
		// This will make sure that if we read the segments we will not need to have this codec available,
		// be it someone trying to read it with external tools, or if  we are accessing and index created with different "versions".
		super( DEFAULT_CODEC.getName(), DEFAULT_CODEC );
		this.knnVectorsFormat = knnVectorsFormat;
	}

	@Override
	public KnnVectorsFormat knnVectorsFormat() {
		return knnVectorsFormat;
	}

	private static class IndexModelBasedPerFieldKnnVectorsFormat extends PerFieldKnnVectorsFormat {

		private final LuceneIndexModel model;

		private IndexModelBasedPerFieldKnnVectorsFormat(LuceneIndexModel model) {
			this.model = model;
		}

		@Override
		public KnnVectorsFormat getKnnVectorsFormatForField(String fieldName) {
			LuceneIndexField field = model.fieldOrNull( fieldName, IndexFieldFilter.ALL );
			if ( field != null ) {
				LuceneFieldCodec<?> codec = field.toValueField().type().codec();
				if ( codec instanceof LuceneVectorFieldCodec ) {
					KnnVectorsFormat knnVectorsFormat = ( (LuceneVectorFieldCodec<?>) codec ).knnVectorFormat();
					if ( knnVectorsFormat != null ) {
						return knnVectorsFormat;
					}
				}
			}
			// we shouldn't really reach this point. Vector fields redefine their vector formats.
			return HibernateSearchKnnVectorsFormat.defaultFormat();
		}
	}
}
