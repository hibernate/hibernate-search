/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.codec.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.perfield.PerFieldKnnVectorsFormat;

public class HibernateSearchLuceneCodec extends FilterCodec {
	private final KnnVectorsFormat knnVectorsFormat;

	public HibernateSearchLuceneCodec(LuceneIndexModel model) {
		super( HibernateSearchLuceneCodec.class.getSimpleName(), Codec.getDefault() );
		this.knnVectorsFormat = new IndexModelBasedPerFieldKnnVectorsFormat( model );
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
				KnnVectorsFormat knnVectorsFormat = field.toValueField().type().codec().knnVectorFormat();
				if ( knnVectorsFormat != null ) {
					return knnVectorsFormat;
				}
			}
			// we shouldn't really reach this point. Vector fields redefine their vector formats.
			return HibernateSearchKnnVectorsFormat.defaultFormat();
		}
	}
}
