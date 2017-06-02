/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.settings.impl.translation.ElasticsearchAnalyzerDefinitionTranslator;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class ScopedElasticsearchAnalyzerReference extends ElasticsearchAnalyzerReference implements ScopedAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private final ElasticsearchAnalyzerReference globalAnalyzerReference;
	private final Map<String, ElasticsearchAnalyzerReference> scopedAnalyzerReferences;

	public ScopedElasticsearchAnalyzerReference(Builder builder) {
		this.globalAnalyzerReference = builder.globalAnalyzerReference;
		this.scopedAnalyzerReferences = Collections.unmodifiableMap( new HashMap<>( builder.scopedAnalyzerReferences ) );
	}

	@Override
	public String getAnalyzerName(String fieldName) {
		return getDelegate( fieldName ).getAnalyzerName( fieldName );
	}

	@Override
	public boolean isNormalizer(String fieldName) {
		return getDelegate( fieldName ).isNormalizer( fieldName );
	}

	@Override
	public void registerDefinitions(String fieldName, ElasticsearchAnalysisDefinitionRegistry definitionRegistry) {
		getDelegate( fieldName ).registerDefinitions( fieldName, definitionRegistry );
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public void initialize(ElasticsearchAnalysisDefinitionRegistry definitionRegistry, ElasticsearchAnalyzerDefinitionTranslator translator) {
		// Nothing to do
	}

	@Override
	public CopyBuilder startCopy() {
		return new Builder( globalAnalyzerReference, scopedAnalyzerReferences );
	}

	private ElasticsearchAnalyzerReference getDelegate(String fieldName) {
		ElasticsearchAnalyzerReference analyzerReference = scopedAnalyzerReferences.get( fieldName );
		if ( analyzerReference == null ) {
			analyzerReference = globalAnalyzerReference;
		}
		return analyzerReference;
	}

	public static class Builder implements ScopedAnalyzerReference.Builder, ScopedAnalyzerReference.CopyBuilder {

		private ElasticsearchAnalyzerReference globalAnalyzerReference;
		private final Map<String, ElasticsearchAnalyzerReference> scopedAnalyzerReferences = new HashMap<>();

		public Builder(ElasticsearchAnalyzerReference globalAnalyzerReference,
				Map<String, ElasticsearchAnalyzerReference> scopedAnalyzers) {
			this.globalAnalyzerReference = globalAnalyzerReference;
			this.scopedAnalyzerReferences.putAll( scopedAnalyzers );
		}

		@Override
		public ElasticsearchAnalyzerReference getGlobalAnalyzerReference() {
			return globalAnalyzerReference;
		}

		@Override
		public void setGlobalAnalyzerReference(AnalyzerReference globalAnalyzerReference) {
			this.globalAnalyzerReference = getElasticsearchAnalyzerReference( globalAnalyzerReference );
		}

		@Override
		public void addAnalyzerReference(String scope, AnalyzerReference analyzerReference) {
			scopedAnalyzerReferences.put( scope, getElasticsearchAnalyzerReference( analyzerReference ) );
		}

		@Override
		public ScopedElasticsearchAnalyzerReference build() {
			return new ScopedElasticsearchAnalyzerReference( this );
		}
	}

	private static ElasticsearchAnalyzerReference getElasticsearchAnalyzerReference(AnalyzerReference analyzerReference) {
		if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
			throw LOG.analyzerReferenceIsNotRemote( analyzerReference );
		}

		return analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
	}

}
