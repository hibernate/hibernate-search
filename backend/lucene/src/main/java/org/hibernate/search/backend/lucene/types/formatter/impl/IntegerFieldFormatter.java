/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.formatter.impl;

import java.util.Objects;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldFormatter;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;

public final class IntegerFieldFormatter implements LuceneFieldFormatter<Integer> {

	private final Store store;

	private final Sortable sortable;

	public IntegerFieldFormatter(Store store, Sortable sortable) {
		this.store = store;
		this.sortable = sortable;
	}

	@Override
	public void addFields(LuceneDocumentBuilder documentBuilder, LuceneIndexSchemaObjectNode parentNode, String fieldName, Integer value) {
		if ( value == null ) {
			return;
		}

		if ( Store.YES.equals( store ) ) {
			documentBuilder.addField( parentNode, new StoredField( fieldName, value ) );
		}

		if ( Sortable.YES.equals( sortable ) ) {
			documentBuilder.addField( parentNode, new NumericDocValuesField( fieldName, value.longValue() ) );
		}

		documentBuilder.addField( parentNode, new IntPoint( fieldName, value ) );
	}

	@Override
	public Integer parse(Document document, String fieldName) {
		IndexableField field = document.getField( fieldName );

		if ( field == null ) {
			return null;
		}

		return (Integer) field.numericValue();
	}

	@Override
	public Object format(Object value) {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( IntegerFieldFormatter.class != obj.getClass() ) {
			return false;
		}

		IntegerFieldFormatter other = (IntegerFieldFormatter) obj;

		return Objects.equals( store, other.store ) && Objects.equals( sortable, other.sortable );
	}

	@Override
	public int hashCode() {
		return Objects.hash( store, sortable );
	}
}
