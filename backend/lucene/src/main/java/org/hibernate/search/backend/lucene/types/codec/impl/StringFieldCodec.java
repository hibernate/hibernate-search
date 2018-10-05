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
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerUtils;

public final class StringFieldCodec implements LuceneFieldCodec<String> {

	private final Sortable sortable;

	private final FieldType fieldType;

	private final Analyzer normalizer;

	public StringFieldCodec(Sortable sortable, FieldType fieldType, Analyzer normalizer) {
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

		switch ( sortable ) {
			case DEFAULT:
			case NO:
				break;
			case YES:
				documentBuilder.addField( new SortedDocValuesField(
						absoluteFieldPath,
						new BytesRef(
								normalizer != null ? AnalyzerUtils.normalize( normalizer, absoluteFieldPath, value ) :
										value )
				) );
				break;
		}
	}

	@Override
	public String decode(Document document, String absoluteFieldPath) {
		return document.get( absoluteFieldPath );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( StringFieldCodec.class != obj.getClass() ) {
			return false;
		}

		StringFieldCodec other = (StringFieldCodec) obj;

		return Objects.equals( sortable, other.sortable ) &&
				Objects.equals( fieldType, other.fieldType ) &&
				Objects.equals( normalizer, other.normalizer );
	}

	@Override
	public int hashCode() {
		return Objects.hash( sortable, fieldType, normalizer );
	}
}
