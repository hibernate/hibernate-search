/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A {@link ProjectionAccumulator} that can accumulate up to one value, and will throw an exception beyond that.
 *
 * @param <F> The type of (unconverted) field values.
 * @param <V> The type of field values after the projection converter was applied.
 */
public final class SingleValuedProjectionAccumulator<F, V> implements ProjectionAccumulator<F, V, F, V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@SuppressWarnings("rawtypes")
	private static final ProjectionAccumulator.Provider PROVIDER = new ProjectionAccumulator.Provider() {
		private final SingleValuedProjectionAccumulator instance = new SingleValuedProjectionAccumulator();
		@Override
		public ProjectionAccumulator get() {
			return instance;
		}
		@Override
		public boolean isSingleValued() {
			return true;
		}
	};

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	public static <V> ProjectionAccumulator.Provider<V, V> provider() {
		return PROVIDER;
	}

	private SingleValuedProjectionAccumulator() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public F createInitial() {
		return null;
	}

	@Override
	public F accumulate(F accumulated, F value) {
		if ( accumulated != null ) {
			throw log.unexpectedMultiValuedField( accumulated, value );
		}
		return value;
	}

	@Override
	public V finish(F accumulated, ProjectionConverter<? super F, ? extends V> converter,
			FromDocumentValueConvertContext context) {
		return converter.fromDocumentValue( accumulated, context );
	}
}
