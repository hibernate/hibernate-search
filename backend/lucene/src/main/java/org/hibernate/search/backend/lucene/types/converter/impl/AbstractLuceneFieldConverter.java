/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter;

abstract class AbstractLuceneFieldConverter<F, T> implements LuceneFieldConverter<F, T> {

	final UserIndexFieldConverter<F> userConverter;

	AbstractLuceneFieldConverter(UserIndexFieldConverter<F> userConverter) {
		this.userConverter = userConverter;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + userConverter + "]";
	}

	@Override
	public Object convertIndexToProjection(F indexValue, FromIndexFieldValueConvertContext context) {
		return userConverter.convertIndexToProjection( indexValue, context );
	}

	@Override
	public boolean isConvertDslToIndexCompatibleWith(LuceneFieldConverter<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldConverter<?, ?> castedOther = (AbstractLuceneFieldConverter<?, ?>) other;
		return userConverter.isConvertDslToIndexCompatibleWith( castedOther.userConverter );
	}

	@Override
	public boolean isConvertIndexToProjectionCompatibleWith(LuceneFieldConverter<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldConverter<?, ?> castedOther = (AbstractLuceneFieldConverter<?, ?>) other;
		return userConverter.isConvertIndexToProjectionCompatibleWith( castedOther.userConverter );
	}

	@Override
	public boolean isProjectionCompatibleWith(Class<?> projectionType) {
		return userConverter.isProjectionCompatibleWith( projectionType );
	}
}
