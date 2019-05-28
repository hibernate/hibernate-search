/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.loading.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class JavaBeanProjectionHitMapper implements ProjectionHitMapper<PojoReference, Void> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoScopeDelegate<?, ?> scopeDelegate;

	public JavaBeanProjectionHitMapper(PojoScopeDelegate<?, ?> scopeDelegate) {
		this.scopeDelegate = scopeDelegate;
	}

	@Override
	public PojoReference convertReference(DocumentReference reference) {
		return scopeDelegate.toPojoReference( reference );
	}

	@Override
	public Object planLoading(DocumentReference reference) {
		throw log.cannotLoadEntity( reference );
	}

	@Override
	public LoadingResult<Void> loadBlocking() {
		return JavaBeanUnusuableLoadingResult.INSTANCE;
	}

	private static class JavaBeanUnusuableLoadingResult implements LoadingResult<Void> {

		private static final JavaBeanUnusuableLoadingResult INSTANCE = new JavaBeanUnusuableLoadingResult();

		private JavaBeanUnusuableLoadingResult() {
		}

		@Override
		public Void getLoaded(Object key) {
			throw new AssertionFailure(
					"Attempt to load an entity with a key that was never issued."
					+ " There is probably a bug in Hibernate Search, please report it."
			);
		}
	}
}
