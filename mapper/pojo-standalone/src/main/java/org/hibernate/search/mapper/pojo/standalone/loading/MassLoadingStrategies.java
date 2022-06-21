/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading;

import java.util.Map;

import org.hibernate.search.mapper.pojo.standalone.loading.impl.MapMassLoadingStrategy;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class MassLoadingStrategies {

	private MassLoadingStrategies() {
	}

	public static <E, I> MassLoadingStrategy<E, I> from(Map<I, E> map) {
		return new MapMassLoadingStrategy<>( map );
	}
}
