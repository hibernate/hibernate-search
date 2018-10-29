/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Objects;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;

public final class IntegerFieldCodec implements LuceneFieldCodec<Integer> {

	private final Projectable projectable;

	private final Sortable sortable;

	public IntegerFieldCodec(Projectable projectable, Sortable sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Integer value) {
		if ( value == null ) {
			return;
		}

		switch ( projectable ) {
			case DEFAULT:
			case NO:
				break;
			case YES:
				documentBuilder.addField( new StoredField( absoluteFieldPath, value ) );
				break;
		}

		switch ( sortable ) {
			case DEFAULT:
			case NO:
				break;
			case YES:
				documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, value.longValue() ) );
				break;
		}

		documentBuilder.addField( new IntPoint( absoluteFieldPath, value ) );
	}

	@Override
	public Integer decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return (Integer) field.numericValue();
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( IntegerFieldCodec.class != obj.getClass() ) {
			return false;
		}

		IntegerFieldCodec other = (IntegerFieldCodec) obj;

		return Objects.equals( projectable, other.projectable ) && Objects.equals( sortable, other.sortable );
	}
}
