/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PojoPathOrdinals {

	private final Map<String, Integer> ordinalByPath = new HashMap<>();
	private final List<String> pathByOrdinal = new ArrayList<>();

	public Integer toOrdinal(String path) {
		return ordinalByPath.get( path );
	}

	public String toPath(int ordinal) {
		return ordinal < pathByOrdinal.size() ? pathByOrdinal.get( ordinal ) : null;
	}

	public int toExistingOrNewOrdinal(String path) {
		Integer ordinal = ordinalByPath.get( path );
		if ( ordinal != null ) {
			return ordinal;
		}
		pathByOrdinal.add( path );
		ordinal = pathByOrdinal.size() - 1;
		ordinalByPath.put( path, ordinal );
		return ordinal;
	}
}
