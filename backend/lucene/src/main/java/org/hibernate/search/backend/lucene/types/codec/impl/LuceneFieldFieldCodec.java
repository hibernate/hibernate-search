/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

public final class LuceneFieldFieldCodec<V> implements LuceneFieldCodec<V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneFieldContributor<V> fieldContributor;

	private LuceneFieldValueExtractor<V> fieldValueExtractor;

	public LuceneFieldFieldCodec(LuceneFieldContributor<V> fieldContributor, LuceneFieldValueExtractor<V> fieldValueExtractor) {
		this.fieldContributor = fieldContributor;
		this.fieldValueExtractor = fieldValueExtractor;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, V value) {
		if ( value == null ) {
			return;
		}

		fieldContributor.contribute( absoluteFieldPath, value, documentBuilder::addField );
	}

	@Override
	public V decode(Document document, String absoluteFieldPath) {
		if ( fieldValueExtractor == null ) {
			throw log.unsupportedProjection( absoluteFieldPath );
		}

		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return fieldValueExtractor.extract( field );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( LuceneFieldFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneFieldFieldCodec<?> other = (LuceneFieldFieldCodec<?>) obj;

		return Objects.equals( fieldContributor, other.fieldContributor )
				&& Objects.equals( fieldValueExtractor, other.fieldValueExtractor );
	}

	@Override
	public int hashCode() {
		return Objects.hash( fieldContributor, fieldValueExtractor );
	}
}
