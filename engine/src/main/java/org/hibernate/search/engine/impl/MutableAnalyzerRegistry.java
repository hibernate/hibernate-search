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
public class MutableAnalyzerRegistry implements AnalyzerRegistry {

	private final AnalyzerStrategy strategy;

	private final AnalyzerReference defaultReference;

	private final AnalyzerReference passThroughReference;

	private final Map<String, AnalyzerReference> referencesByName = new LinkedHashMap<>();

	private final Map<Class<?>, AnalyzerReference> referencesByLuceneClass = new LinkedHashMap<>();

	private final Collection<AnalyzerReference> scopedReferences = new ArrayList<>();

	MutableAnalyzerRegistry(AnalyzerStrategy strategy) {
		this( strategy, null );
	}

	MutableAnalyzerRegistry(AnalyzerStrategy strategy, AnalyzerRegistry registryState) {
		this.strategy = strategy;

		if ( registryState != null ) {
			this.defaultReference = registryState.getDefaultAnalyzerReference();
			this.passThroughReference = registryState.getPassThroughAnalyzerReference();
			this.referencesByName.putAll( registryState.getNamedAnalyzerReferences() );
			this.referencesByLuceneClass.putAll( registryState.getLuceneClassAnalyzerReferences() );
		}
		else {
			this.defaultReference = strategy.createDefaultAnalyzerReference();
			this.passThroughReference = strategy.createPassThroughAnalyzerReference();
		}
	}

	@Override
	public AnalyzerReference getDefaultAnalyzerReference() {
		return defaultReference;
	}

	@Override
	public AnalyzerReference getPassThroughAnalyzerReference() {
		return passThroughReference;
	}

	@Override
	public Map<String, AnalyzerReference> getNamedAnalyzerReferences() {
		return Collections.unmodifiableMap( referencesByName );
	}

	@Override
	public Map<Class<?>, AnalyzerReference> getLuceneClassAnalyzerReferences() {
		return Collections.unmodifiableMap( referencesByLuceneClass );
	}

	@Override
	public AnalyzerReference getAnalyzerReference(String name) {
		return referencesByName.get( name );
	}

	public AnalyzerReference getOrCreateAnalyzerReference(String name) {
		AnalyzerReference reference = referencesByName.get( name );
		if ( reference == null ) {
			reference = strategy.createNamedAnalyzerReference( name );
			referencesByName.put( name, reference );
		}
		return reference;
	}

	@Override
	public AnalyzerReference getAnalyzerReference(Class<?> analyzerClazz) {
		return referencesByLuceneClass.get( analyzerClazz );
	}

	public AnalyzerReference getOrCreateAnalyzerReference(Class<?> analyzerClazz) {
		AnalyzerReference reference = referencesByLuceneClass.get( analyzerClazz );
		if ( reference == null ) {
			reference = strategy.createLuceneClassAnalyzerReference( analyzerClazz );
			referencesByLuceneClass.put( analyzerClazz, reference );
		}
		return reference;
	}

	@Override
	public void close() {
		close( referencesByLuceneClass.values() );
	}

	private void close(Collection<AnalyzerReference> references) {
		for ( AnalyzerReference reference : references ) {
			reference.close();
		}
	}

	public void initialize(Map<String, AnalyzerDef> analyzerDefinitions) {
		List<AnalyzerReference> references = new ArrayList<>();
		references.add( defaultReference );
		references.add( passThroughReference );
		references.addAll( referencesByName.values() );
		references.addAll( referencesByLuceneClass.values() );
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