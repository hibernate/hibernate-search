/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common.logging;

public class EnumFormatter {

	private final String formatted;

	public EnumFormatter(Enum value) {
		this.formatted = value.name();
	}

	@Override
	public String toString() {
		return formatted;
	}
}
