/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

public final class LuceneFieldFieldCodec<V> implements LuceneFieldCodec<V> {

	private BiFunction<String, V, IndexableField> fieldProducer;

	private Function<IndexableField, V> fieldValueExtractor;

	public LuceneFieldFieldCodec(BiFunction<String, V, IndexableField> fieldProducer, Function<IndexableField, V> fieldValueExtractor) {
		this.fieldProducer = fieldProducer;
		this.fieldValueExtractor = fieldValueExtractor;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, V value) {
		if ( value == null ) {
			return;
		}

		documentBuilder.addField( fieldProducer.apply( absoluteFieldPath, value ) );
	}

	@Override
	public V decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return fieldValueExtractor.apply( field );
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

		return Objects.equals( fieldProducer, other.fieldProducer )
				&& Objects.equals( fieldValueExtractor, other.fieldValueExtractor );
	}

	@Override
	public int hashCode() {
		return Objects.hash( fieldProducer, fieldValueExtractor );
	}
}
