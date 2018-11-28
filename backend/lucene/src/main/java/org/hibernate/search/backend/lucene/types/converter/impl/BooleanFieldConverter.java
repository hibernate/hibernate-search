/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserIndexFieldConverter;

public final class BooleanFieldConverter extends AbstractFieldConverter<Boolean, Integer> {

	public BooleanFieldConverter(UserIndexFieldConverter<Boolean> userConverter) {
		super( userConverter );
	}

	@Override
	public Integer convertDslToIndex(Object value,
			ToIndexFieldValueConvertContext context) {
		Boolean rawValue = userConverter.convertDslToIndex( value, context );
		if ( value == null ) {
			return null;
		}
		return ( rawValue ) ? 1 : 0;
	}
}
