/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public final class MappableTypeModelFormatter {

	private final MappableTypeModel typeModel;

	public MappableTypeModelFormatter(MappableTypeModel typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public String toString() {
		return typeModel.getName();
	}
}
