/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference.impl;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.reference.SearchValueFieldReference;
import org.hibernate.search.engine.search.reference.ValueFieldReference;

public class ValueFieldReferenceImpl<T, V, P> extends TypedFieldReferenceImpl<T, P>
		implements ValueFieldReference<T, V, P> {

	private final TypedFieldReferenceImpl<V, V> noConverter;
	private final TypedFieldReferenceImpl<String, String> string;

	public ValueFieldReferenceImpl(String absolutePath, Class<T> inputType, Class<V> indexType, Class<P> projectionType) {
		super( absolutePath, ValueConvert.YES, inputType, projectionType );
		this.noConverter = new TypedFieldReferenceImpl<>( absolutePath, ValueConvert.NO, indexType, indexType );
		this.string = new TypedFieldReferenceImpl<>( absolutePath, ValueConvert.PARSE, String.class, String.class );
	}

	@Override
	public SearchValueFieldReference<V, V> noConverter() {
		return noConverter;
	}


	@Override
	public SearchValueFieldReference<String, String> asString() {
		return string;
	}

}
