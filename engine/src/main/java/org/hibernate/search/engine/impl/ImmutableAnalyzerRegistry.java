/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;

/**
 * An immutable {@link AnalyzerRegistry}.
 *
 * @author Yoann Rodiere
 */
public class ImmutableAnalyzerRegistry implements AnalyzerRegistry {

	private final AnalyzerReference defaultReference;

	private final AnalyzerReference passThroughReference;

	private final Map<String, AnalyzerReference> referencesByName;

	private final Map<Class<?>, AnalyzerReference> referencesByClass;

	ImmutableAnalyzerRegistry(AnalyzerRegistry registryState) {
		this.defaultReference = registryState.getDefaultAnalyzerReference();
		this.passThroughReference = registryState.getPassThroughAnalyzerReference();
		this.referencesByName = Collections.unmodifiableMap( new HashMap<>( registryState.getNamedAnalyzerReferences() ) );
		this.referencesByClass = Collections.unmodifiableMap( new HashMap<>( registryState.getLuceneClassAnalyzerReferences() ) );
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
		return referencesByName;
	}

	@Override
	public Map<Class<?>, AnalyzerReference> getLuceneClassAnalyzerReferences() {
		return referencesByClass;
	}

	@Override
	public AnalyzerReference getAnalyzerReference(String name) {
		return referencesByName.get( name );
	}

	@Override
	public AnalyzerReference getAnalyzerReference(Class<?> analyzerClazz) {
		return referencesByClass.get( analyzerClazz );
	}

	@Override
	public void close() {
		close( referencesByClass.values() );
	}

	private void close(Collection<AnalyzerReference> references) {
		for ( AnalyzerReference reference : references ) {
			reference.close();
		}
	}

}