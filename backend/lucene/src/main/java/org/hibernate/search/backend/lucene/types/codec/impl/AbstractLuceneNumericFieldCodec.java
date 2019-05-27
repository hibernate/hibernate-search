/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public abstract class AbstractLuceneNumericFieldCodec<F, E extends Number> implements LuceneStandardFieldCodec<F, E> {

	private final boolean projectable;
	private final boolean searchable;
	private final boolean sortable;

	private final F indexNullAsValue;

	public AbstractLuceneNumericFieldCodec(boolean projectable, boolean searchable, boolean sortable, F indexNullAsValue) {
		this.projectable = projectable;
		this.searchable = searchable;
		this.sortable = sortable;
		this.indexNullAsValue = indexNullAsValue;
	}

	@Override
	public final void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, F value) {
		if ( value == null && indexNullAsValue != null ) {
			value = indexNullAsValue;
		}

		if ( value == null ) {
			return;
		}

		E encodedValue = encode( value );

		if ( projectable ) {
			doEncodeForProjection( documentBuilder, absoluteFieldPath, value, encodedValue );
		}

		LuceneNumericDomain<E> domain = getDomain();

		if ( sortable ) {
			documentBuilder.addField( domain.createDocValuesField( absoluteFieldPath, encodedValue ) );
		}
		else {
			// For createExistsQuery()
			documentBuilder.addFieldName( absoluteFieldPath );
		}

		if ( searchable ) {
			documentBuilder.addField( domain.createIndexField( absoluteFieldPath, encodedValue ) );
		}
	}

	@Override
	public Query createExistsQuery(String absoluteFieldPath) {
		if ( sortable ) {
			return new DocValuesFieldExistsQuery( absoluteFieldPath );
		}
		else {
			return new TermQuery( new Term( LuceneFields.fieldNamesFieldName(), absoluteFieldPath ) );
		}
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}

		AbstractLuceneNumericFieldCodec<?, ?> other = (AbstractLuceneNumericFieldCodec<?, ?>) obj;

		return projectable == other.projectable && sortable == other.sortable;
	}

	public abstract LuceneNumericDomain<E> getDomain();

	abstract void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath,
			F value, E encodedValue);

}
