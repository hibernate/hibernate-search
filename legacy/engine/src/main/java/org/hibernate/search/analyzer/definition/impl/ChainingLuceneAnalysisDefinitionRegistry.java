/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.NormalizerDef;

/**
 * An {@link LuceneAnalysisDefinitionRegistry} that delegate calls
 * to a "parent" registry when no definition is found.
 * <p>
 * Mutating calls ({@code register} methods) are never delegated to the parent.
 *
 * @author Yoann Rodiere
 */
public final class ChainingLuceneAnalysisDefinitionRegistry implements LuceneAnalysisDefinitionRegistry {

	private final LuceneAnalysisDefinitionRegistry parent;
	private final LuceneAnalysisDefinitionRegistry self;

	public ChainingLuceneAnalysisDefinitionRegistry(LuceneAnalysisDefinitionRegistry self,
			LuceneAnalysisDefinitionRegistry parent) {
		this.parent = parent;
		this.self = self;
	}

	@Override
	public void register(String name, AnalyzerDef definition) {
		self.register( name, definition );
	}

	@Override
	public void register(String name, NormalizerDef definition) {
		self.register( name, definition );
	}

	@Override
	public AnalyzerDef getAnalyzerDefinition(String name) {
		AnalyzerDef result = self.getAnalyzerDefinition( name );
		if ( result == null ) {
			result = parent.getAnalyzerDefinition( name );
		}
		return result;
	}

	@Override
	public NormalizerDef getNormalizerDefinition(String name) {
		NormalizerDef result = self.getNormalizerDefinition( name );
		if ( result == null ) {
			result = parent.getNormalizerDefinition( name );
		}
		return result;
	}

}
