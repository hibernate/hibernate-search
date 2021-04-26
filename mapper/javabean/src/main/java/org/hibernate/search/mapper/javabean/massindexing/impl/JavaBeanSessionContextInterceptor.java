/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.massindexing.impl;

import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

public class JavaBeanSessionContextInterceptor implements LoadingInterceptor {

	private final JavaBeanMassIndexingMappingContext mappingContext;

	public static LoadingInterceptor of(JavaBeanMassIndexingMappingContext mappingContext) {
		return new JavaBeanSessionContextInterceptor( mappingContext );
	}

	JavaBeanSessionContextInterceptor(JavaBeanMassIndexingMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public void intercept(LoadingInvocationContext ictx) throws Exception {
		PojoMassIndexingSessionContext sessionContext = mappingContext.sessionContext();
		ictx.context( PojoMassIndexingSessionContext.class, sessionContext );
		ictx.proceed();
	}
}
