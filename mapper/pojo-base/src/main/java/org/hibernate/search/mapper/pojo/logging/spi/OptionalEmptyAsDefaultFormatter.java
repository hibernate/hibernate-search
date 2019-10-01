/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.logging.spi;

import java.util.Optional;

public class OptionalEmptyAsDefaultFormatter {

	private final String formatted;

	public OptionalEmptyAsDefaultFormatter(Optional<?> optional) {
		this.formatted = optional.isPresent() ? optional.get().toString() : "<default>";
	}

	@Override
	public String toString() {
		return formatted;
	}
}
