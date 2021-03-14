/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading;

import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingOptions;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;

public interface LoadingOptions {

	<T> LoadingOptions registerLoader(Class<T> type, EntityLoader<T> loader);

	<T> void massIndexingLoadingStrategy(Class<T> type, MassIndexingEntityLoadingStrategy<T, JavaBeanIndexingOptions> loadingStrategy);

	void identifierInterceptor(LoadingInterceptor<JavaBeanIndexingOptions> interceptor);

	void documentInterceptor(LoadingInterceptor<JavaBeanIndexingOptions> interceptor);
}
