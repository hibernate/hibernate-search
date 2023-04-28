/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.bridgeresolver;

public class EnumLabelService {
	public String toLabel(Object value) {
		switch ( (Genre) value ) {
			case SCIFI:
				return "science-fiction";
			case CRIME:
				return "crime-fiction";
		}
		throw new IllegalArgumentException( "Unsupported value: " + value );
	}

	public <T> T fromLabel(Class<T> enumType, String value) {
		if ( Genre.class.equals( enumType ) ) {
			switch ( value ) {
				case "science-fiction":
					return enumType.cast( Genre.SCIFI );
				case "crime-fiction":
					return enumType.cast( Genre.CRIME );
			}
		}

		throw new IllegalArgumentException( "Unsupported type/value: " + enumType + "/" + value );
	}
}
