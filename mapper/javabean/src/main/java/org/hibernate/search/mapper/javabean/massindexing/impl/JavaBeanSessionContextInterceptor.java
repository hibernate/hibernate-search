/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.massindexing.impl;

import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingOptions;
import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingSessionContext;

public class JavaBeanSessionContextInterceptor implements LoadingInterceptor<JavaBeanIndexingOptions> {

	private final JavaBeanMassIndexingMappingContext mappingContext;

	public static LoadingInterceptor of(JavaBeanMassIndexingMappingContext mappingContext) {
		return new JavaBeanSessionContextInterceptor( mappingContext );
	}

	JavaBeanSessionContextInterceptor(JavaBeanMassIndexingMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public void intercept(LoadingInvocationContext<JavaBeanIndexingOptions> ictx) throws Exception {
		MassIndexingSessionContext sessionContext = mappingContext.sessionContext();
		ictx.contextData().put( MassIndexingSessionContext.class, sessionContext );
		ictx.proceed();
	}
}
