/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference.impl;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.reference.SearchValueFieldReference;

public class TypedFieldReferenceImpl<T, P> implements SearchValueFieldReference<T, P> {

	private final String absolutePath;
	private final ValueConvert valueConvert;
	private final Class<T> input;
	private final Class<P> projection;

	public TypedFieldReferenceImpl(String absolutePath, ValueConvert valueConvert, Class<T> input, Class<P> projection) {
		this.absolutePath = absolutePath;
		this.valueConvert = valueConvert;
		this.input = input;
		this.projection = projection;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public ValueConvert valueConvert() {
		return valueConvert;
	}

	@Override
	public Class<P> projectionType() {
		return projection;
	}

	@Override
	public Class<T> type() {
		return input;
	}
}
