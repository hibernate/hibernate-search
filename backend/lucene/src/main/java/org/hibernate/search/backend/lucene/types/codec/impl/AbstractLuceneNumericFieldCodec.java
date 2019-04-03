/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

public abstract class AbstractLuceneNumericFieldCodec<F, E extends Number> implements LuceneStandardFieldCodec<F, E> {

	private final boolean projectable;

	private final boolean sortable;

	public AbstractLuceneNumericFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public final void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, F value) {
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

		documentBuilder.addField( domain.createIndexField( absoluteFieldPath, encodedValue ) );

		// For "exists" predicates
		documentBuilder.addFieldName( absoluteFieldPath );
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
