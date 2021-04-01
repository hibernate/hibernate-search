/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import org.hibernate.search.mapper.javabean.loading.LoadingOptions;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContextBuilder;

public interface JavaBeanLoadingContextBuilder extends PojoLoadingContextBuilder<LoadingOptions>,
		PojoMassIndexingContextBuilder<LoadingOptions> {

	@Override
	JavaBeanSearchLoadingContext build();

}
