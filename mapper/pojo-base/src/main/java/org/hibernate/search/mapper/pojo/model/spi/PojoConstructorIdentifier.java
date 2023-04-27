/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;

public final class PojoConstructorIdentifier {
	private final String name;
	private final Class<?>[] parametersJavaTypes;

	public PojoConstructorIdentifier(PojoConstructorModel<?> constructor) {
		this.name = constructor.typeModel().name();
		this.parametersJavaTypes = constructor.parametersJavaTypes();
	}

	public String toHighlightedString(int position) {
		return name + "(" + CommaSeparatedClassesFormatter.formatHighlighted( parametersJavaTypes, position ) + ")";
	}

	@Override
	public String toString() {
		return toHighlightedString( -1 );
	}
}
