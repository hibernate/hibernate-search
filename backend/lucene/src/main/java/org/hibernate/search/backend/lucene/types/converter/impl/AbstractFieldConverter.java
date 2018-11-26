/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter;

abstract class AbstractFieldConverter<F, T> implements LuceneFieldConverter<F, T> {

	final UserIndexFieldConverter<F> userConverter;

	AbstractFieldConverter(UserIndexFieldConverter<F> userConverter) {
		this.userConverter = userConverter;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + userConverter + "]";
	}

	@Override
	public Object convertFromProjection(F value, FromIndexFieldValueConvertContext context) {
		return userConverter.convertFromProjection( value, context );
	}

	@Override
	public boolean isConvertFromDslCompatibleWith(LuceneFieldConverter<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractFieldConverter<?, ?> castedOther = (AbstractFieldConverter<?, ?>) other;
		return userConverter.isConvertFromDslCompatibleWith( castedOther.userConverter );
	}

	@Override
	public boolean isConvertFromProjectionCompatibleWith(LuceneFieldConverter<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractFieldConverter<?, ?> castedOther = (AbstractFieldConverter<?, ?>) other;
		return userConverter.isConvertFromProjectionCompatibleWith( castedOther.userConverter );
	}

	@Override
	public boolean isProjectionCompatibleWith(Class<?> projectionType) {
		return userConverter.isProjectionCompatibleWith( projectionType );
	}
}
