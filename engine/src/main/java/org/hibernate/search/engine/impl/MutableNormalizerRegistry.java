/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.util.impl.Closeables;

/**
 * This class gives access to the set of normalizer references created for a given index manager type,
 * creating new ones as necessary using the {@link AnalyzerStrategy} of that index manager type.
 *
 * @author Yoann Rodiere
 */
public class MutableNormalizerRegistry implements NormalizerRegistry {

	private final AnalyzerStrategy strategy;

	private final Map<String, AnalyzerReference> referencesByName = new LinkedHashMap<>();

	private final Map<Class<?>, AnalyzerReference> referencesByLuceneClass = new LinkedHashMap<>();

	MutableNormalizerRegistry(AnalyzerStrategy strategy) {
		this( strategy, null );
	}

	MutableNormalizerRegistry(AnalyzerStrategy strategy, NormalizerRegistry registryState) {
		this.strategy = strategy;

		if ( registryState != null ) {
			this.referencesByName.putAll( registryState.getNamedNormalizerReferences() );
			this.referencesByLuceneClass.putAll( registryState.getLuceneClassNormalizerReferences() );
		}
	}

	@Override
	public Map<String, AnalyzerReference> getNamedNormalizerReferences() {
		return Collections.unmodifiableMap( referencesByName );
	}

	@Override
	public Map<Class<?>, AnalyzerReference> getLuceneClassNormalizerReferences() {
		return Collections.unmodifiableMap( referencesByLuceneClass );
	}

	@Override
	public AnalyzerReference getNamedNormalizerReference(String name) {
		return referencesByName.get( name );
	}

	public AnalyzerReference getOrCreateNamedNormalizerReference(String name) {
		AnalyzerReference reference = referencesByName.get( name );
		if ( reference == null ) {
			reference = strategy.createNamedNormalizerReference( name );
			referencesByName.put( name, reference );
		}
		return reference;
	}

	@Override
	public AnalyzerReference getLuceneClassNormalizerReference(Class<?> analyzerClazz) {
		return referencesByLuceneClass.get( analyzerClazz );
	}

	public AnalyzerReference getOrCreateLuceneClassNormalizerReference(Class<?> analyzerClazz) {
		AnalyzerReference reference = referencesByLuceneClass.get( analyzerClazz );
		if ( reference == null ) {
			reference = strategy.createLuceneClassNormalizerReference( analyzerClazz );
			referencesByLuceneClass.put( analyzerClazz, reference );
		}
		return reference;
	}

	@Override
	public void close() {
		Closeables.closeQuietly( getAllReferences() );
	}

	public List<AnalyzerReference> getAllReferences() {
		List<AnalyzerReference> references = new ArrayList<>();
		references.addAll( referencesByName.values() );
		references.addAll( referencesByLuceneClass.values() );
		return references;
	}

}