/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.massindexing.loader;

import java.util.Collection;
import java.util.Map;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanCollectionIndexingStrategy;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMapIndexingStrategy;
import org.hibernate.search.mapper.pojo.loading.EntityLoadingTypeGroup;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;

public class JavaBeanIndexingStrategies {

	private JavaBeanIndexingStrategies() {
	}

	public static <T> MassIndexingEntityLoadingStrategy<T, JavaBeanIndexingOptions> from(Map<?, T> map) {
		return JavaBeanIndexingStrategies.from( map, EntityLoadingTypeGroup.asIstanceOf() );
	}

	public static <T> MassIndexingEntityLoadingStrategy<T, JavaBeanIndexingOptions> from(Map<?, T> map,
			EntityLoadingTypeGroup group) {
		return new JavaBeanMapIndexingStrategy( map, group );
	}

	public static <T> MassIndexingEntityLoadingStrategy<T, JavaBeanIndexingOptions> from(Collection<T> collection) {
		return JavaBeanIndexingStrategies.from( collection, EntityLoadingTypeGroup.asIstanceOf() );
	}

	public static <T> MassIndexingEntityLoadingStrategy<T, JavaBeanIndexingOptions> from(Collection<T> collection,
			EntityLoadingTypeGroup group) {
		return new JavaBeanCollectionIndexingStrategy( collection, group );
	}

}
