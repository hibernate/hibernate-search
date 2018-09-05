/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.util.impl.Closeables;

/**
 * An immutable {@link NormalizerRegistry}.
 *
 * @author Yoann Rodiere
 */
public class ImmutableNormalizerRegistry implements NormalizerRegistry {

	private final Map<String, AnalyzerReference> referencesByName;

	private final Map<Class<?>, AnalyzerReference> referencesByClass;

	ImmutableNormalizerRegistry(NormalizerRegistry registryState) {
		this.referencesByName = Collections.unmodifiableMap( new HashMap<>( registryState.getNamedNormalizerReferences() ) );
		this.referencesByClass = Collections.unmodifiableMap( new HashMap<>( registryState.getLuceneClassNormalizerReferences() ) );
	}

	@Override
	public Map<String, AnalyzerReference> getNamedNormalizerReferences() {
		return referencesByName;
	}

	@Override
	public Map<Class<?>, AnalyzerReference> getLuceneClassNormalizerReferences() {
		return referencesByClass;
	}

	@Override
	public AnalyzerReference getNamedNormalizerReference(String name) {
		return referencesByName.get( name );
	}

	@Override
	public AnalyzerReference getLuceneClassNormalizerReference(Class<?> analyzerClazz) {
		return referencesByClass.get( analyzerClazz );
	}

	@Override
	public void close() {
		Closeables.closeQuietly( referencesByName.values(), referencesByClass.values() );
	}

}