/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;

public final class PojobTypeAndModelPath {
	public final MappableTypeModel type;
	public final PojoModelPath path;

	public PojobTypeAndModelPath(MappableTypeModel type, PojoModelPath path) {
		this.type = type;
		this.path = path;
	}

	@Override
	public String toString() {
		return type.name() + "#" + path;
	}
}
