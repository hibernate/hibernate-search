/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Objects;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerUtils;

public final class LuceneStringFieldCodec implements LuceneFieldCodec<String> {

	private final boolean sortable;

	private final FieldType fieldType;

	private final Analyzer normalizer;

	public LuceneStringFieldCodec(boolean sortable, FieldType fieldType, Analyzer normalizer) {
		this.sortable = sortable;
		this.fieldType = fieldType;
		this.normalizer = normalizer;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, String value) {
		if ( value == null ) {
			return;
		}

		documentBuilder.addField( new Field( absoluteFieldPath, value, fieldType ) );

		if ( sortable ) {
			documentBuilder.addField( new SortedDocValuesField(
					absoluteFieldPath,
					new BytesRef(
							normalizer != null ? AnalyzerUtils.normalize( normalizer, absoluteFieldPath, value ) :
									value )
			) );
		}
	}

	@Override
	public String decode(Document document, String absoluteFieldPath) {
		return document.get( absoluteFieldPath );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneStringFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneStringFieldCodec other = (LuceneStringFieldCodec) obj;

		return ( sortable == other.sortable ) &&
				Objects.equals( fieldType, other.fieldType ) &&
				Objects.equals( normalizer, other.normalizer );
	}
}
