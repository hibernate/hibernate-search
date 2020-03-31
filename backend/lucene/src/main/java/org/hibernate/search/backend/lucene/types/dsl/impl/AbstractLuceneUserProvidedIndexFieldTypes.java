/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneDoubleFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFloatFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneIntegerFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLongFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;

/**
 *
 * @author Waldemar Kłaczyński
 */
public abstract class AbstractLuceneUserProvidedIndexFieldTypes {

	private final boolean projectable;
	private final boolean searchable;
	private final boolean sortable;
	private final boolean aggregable;

	public AbstractLuceneUserProvidedIndexFieldTypes(boolean projectable, boolean searchable, boolean sortable, boolean aggregable) {
		this.projectable = projectable;
		this.searchable = searchable;
		this.sortable = sortable;
		this.aggregable = aggregable;
	}

	protected <E extends Number> LuceneStandardFieldCodec<?, E> getCodec(Class<E> type, E indexNullAsValue) {
		LuceneStandardFieldCodec codec = null;

		if ( Double.class.isAssignableFrom( type ) ) {
			codec = new LuceneDoubleFieldCodec(
				projectable, searchable, sortable, aggregable, (Double) indexNullAsValue );
		}
		else if ( Float.class.isAssignableFrom( type ) ) {
			codec = new LuceneFloatFieldCodec(
				projectable, searchable, sortable, aggregable, (Float) indexNullAsValue );
		}
		else if ( Long.class.isAssignableFrom( type ) ) {
			codec = new LuceneLongFieldCodec(
				projectable, searchable, sortable, aggregable, (Long) indexNullAsValue );
		}
		else if ( Integer.class.isAssignableFrom( type ) ) {
			codec = new LuceneIntegerFieldCodec(
				projectable, searchable, sortable, aggregable, (Integer) indexNullAsValue );
		}

		return codec;

	}

}
