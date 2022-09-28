/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import java.util.Optional;

public final class PojoPathEntityStateRepresentation {

	private final int ordinalInStateArray;
	private final Optional<BindablePojoModelPath> pathFromStateArrayElement;

	public PojoPathEntityStateRepresentation(int ordinalInStateArray,
			Optional<BindablePojoModelPath> pathFromStateArrayElement) {
		this.ordinalInStateArray = ordinalInStateArray;
		this.pathFromStateArrayElement = pathFromStateArrayElement;
	}

	public int ordinalInStateArray() {
		return ordinalInStateArray;
	}

	public Optional<BindablePojoModelPath> pathFromStateArrayElement() {
		return pathFromStateArrayElement;
	}
}
