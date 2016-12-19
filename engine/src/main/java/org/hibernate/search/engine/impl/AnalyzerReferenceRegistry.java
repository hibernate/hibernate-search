/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference.Builder;
import org.hibernate.search.annotations.AnalyzerDef;

/**
 * This class gives access to the set of analyzer references created for a given index manager type,
 * creating new ones as necessary using the {@link AnalyzerStrategy} of that index manager type.
 *
 * @author Yoann Rodiere
 */
public class AnalyzerReferenceRegistry {

	private final AnalyzerStrategy strategy;

	private final AnalyzerReference defaultReference;

	private final AnalyzerReference passThroughReference;

	private final Map<String, AnalyzerReference> referencesByName = new LinkedHashMap<>();

	private final Map<Class<?>, AnalyzerReference> referencesByClass = new LinkedHashMap<>();

	private final Collection<AnalyzerReference> scopedReferences = new ArrayList<>();

	AnalyzerReferenceRegistry(AnalyzerStrategy strategy) {
		this.strategy = strategy;
		this.defaultReference = strategy.createDefaultAnalyzerReference();
		this.passThroughReference = strategy.createPassThroughAnalyzerReference();
	}

	public AnalyzerReference getDefaultAnalyzerReference() {
		return defaultReference;
	}

	public AnalyzerReference getPassThroughAnalyzerReference() {
		return passThroughReference;
	}

	public Map<String, AnalyzerReference> getAnalyzerReferencesByName() {
		return Collections.unmodifiableMap( referencesByName );
	}

	public Map<Class<?>, AnalyzerReference> getAnalyzerReferencesByClass() {
		return Collections.unmodifiableMap( referencesByClass );
	}

	public AnalyzerReference getAnalyzerReference(String name) {
		AnalyzerReference reference = referencesByName.get( name );
		if ( reference == null ) {
			reference = strategy.createNamedAnalyzerReference( name );
			referencesByName.put( name, reference );
		}
		return reference;
	}

	public AnalyzerReference getAnalyzerReference(Class<?> analyzerClazz) {
		AnalyzerReference reference = referencesByClass.get( analyzerClazz );
		if ( reference == null ) {
			reference = strategy.createAnalyzerReference( analyzerClazz );
			referencesByClass.put( analyzerClazz, reference );
		}
		return reference;
	}

	public void initialize(Map<String, AnalyzerDef> analyzerDefinitions) {
		List<AnalyzerReference> references = new ArrayList<>();
		references.add( defaultReference );
		references.add( passThroughReference );
		references.addAll( referencesByName.values() );
		references.addAll( referencesByClass.values() );
		references.addAll( scopedReferences );
		strategy.initializeAnalyzerReferences( references, analyzerDefinitions );
	}

	public ScopedAnalyzerReference.Builder buildScopedAnalyzerReference() {
		return new ScopedAnalyzerReferenceBuilderRegisteringWrapper(
				strategy.buildScopedAnalyzerReference( getDefaultAnalyzerReference() )
				);
	}

	/**
	 * A builder that will delegate to another builder for all operations, but will also add any
	 * built reference to the registry.
	 *
	 * @author Yoann Rodiere
	 */
	private class ScopedAnalyzerReferenceBuilderRegisteringWrapper implements ScopedAnalyzerReference.Builder {

		private final ScopedAnalyzerReference.Builder delegate;

		public ScopedAnalyzerReferenceBuilderRegisteringWrapper(Builder delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public AnalyzerReference getGlobalAnalyzerReference() {
			return delegate.getGlobalAnalyzerReference();
		}

		@Override
		public void setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
			delegate.setGlobalAnalyzerReference( globalAnalyzerReference );
		}

		@Override
		public void addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			delegate.addAnalyzerReference( scope, analyzerReference );
		}

		@Override
		public ScopedAnalyzerReference build() {
			ScopedAnalyzerReference reference = delegate.build();
			// Register the newly built reference
			scopedReferences.add( reference );
			return reference;
		}

	}

}