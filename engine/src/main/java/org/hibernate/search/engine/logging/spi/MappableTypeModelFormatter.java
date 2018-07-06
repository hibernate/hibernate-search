/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public class MappableTypeModelFormatter {

	private final String formatted;

	public MappableTypeModelFormatter(MappableTypeModel typeModel) {
		this.formatted = typeModel.getName();
	}

	@Override
	public String toString() {
		return formatted;
	}
}
