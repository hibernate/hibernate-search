/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.spi;

import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.mapper.session.context.SessionContext;

public final class PassThroughFromIndexFieldValueConverter<F> implements FromIndexFieldValueConverter<F, F> {

	private final Class<F> fieldType;

	public PassThroughFromIndexFieldValueConverter(Class<F> fieldType) {
		this.fieldType = fieldType;
	}

	@Override
	public F convert(F value, SessionContext sessionContext) {
		return value;
	}

	@Override
	public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
		return superTypeCandidate.isAssignableFrom( fieldType );
	}
}
