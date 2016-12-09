/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;

/**
 * This class gives access to the set of analyzer references created for a given index manager type,
 * creating new ones as necessary using the {@link AnalyzerStrategy} of that index manager type.
 *
 * @author Yoann Rodiere
 */
public class AnalyzerReferenceRegistry<T extends AnalyzerReference> {

	private final AnalyzerStrategy<T> strategy;

	private final T defaultReference;

	private final T passThroughReference;

	private final Map<String, T> referencesByName = new LinkedHashMap<>();

	private final Map<Class<?>, T> referencesByClass = new LinkedHashMap<>();

	AnalyzerReferenceRegistry(AnalyzerStrategy<T> strategy) {
		this.strategy = strategy;
		this.defaultReference = strategy.createDefaultAnalyzerReference();
		this.passThroughReference = strategy.createPassThroughAnalyzerReference();
	}

	public T getDefaultAnalyzerReference() {
		return defaultReference;
	}

	public T getPassThroughAnalyzerReference() {
		return passThroughReference;
	}

	public Map<String, T> getAnalyzerReferencesByName() {
		return Collections.unmodifiableMap( referencesByName );
	}

	public Map<Class<?>, T> getAnalyzerReferencesByClass() {
		return Collections.unmodifiableMap( referencesByClass );
	}

	public T getAnalyzerReference(String name) {
		T reference = referencesByName.get( name );
		if ( reference == null ) {
			reference = strategy.createAnalyzerReference( name );
			referencesByName.put( name, reference );
		}
		return reference;
	}

	public T getAnalyzerReference(Class<?> analyzerClazz) {
		T reference = referencesByClass.get( analyzerClazz );
		if ( reference == null ) {
			reference = strategy.createAnalyzerReference( analyzerClazz );
			referencesByClass.put( analyzerClazz, reference );
		}
		return reference;
	}

	public void initialize(Map<String, AnalyzerDef> analyzerDefinitions) {
		strategy.initializeNamedAnalyzerReferences( referencesByName.values(), analyzerDefinitions );
	}

}