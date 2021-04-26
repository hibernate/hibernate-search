/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.massindexing.loader;

import java.util.Map;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMapIndexingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;

public class JavaBeanIndexingStrategies {

	private JavaBeanIndexingStrategies() {
	}

	public static <T> MassIndexingEntityLoadingStrategy<T, JavaBeanIndexingOptions> from(Map<?, T> map) {
		return new JavaBeanMapIndexingStrategy<>( map );
	}
}
