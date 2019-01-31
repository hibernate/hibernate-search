/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.translation.ElasticsearchAnalyzerDefinitionTranslator;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to an {@code ElasticsearchAnalyzer}.
 *
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchAnalyzerReference extends RemoteAnalyzerReference {

	private final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	/**
	 * Register definitions that will be needed in order to add the field named {@code fieldName}
	 * to the Elasticsearch mapping.
	 *
	 * @param fieldName The name of the field for which every referenced analysis definition should be
	 * registered.
	 * @param definitionRegistry The registry to be populated (it may be empty).
	 */
	public abstract void registerDefinitions(String fieldName,
			ElasticsearchAnalysisDefinitionRegistry definitionRegistry);

	public abstract boolean isInitialized();

	/**
	 * Initialize the internals of this reference, so that enough information will be available
	 * to execute {@link #getAnalyzerName(String)} and
	 * {@link #registerDefinitions(String, ElasticsearchAnalysisDefinitionRegistry)}.
	 * @param definitionRegistry The registry holding all known analyzer definitions.
	 * @param translator An {@link ElasticsearchAnalyzerDefinitionTranslator}.
	 */
	public abstract void initialize(ElasticsearchAnalysisDefinitionRegistry definitionRegistry,
			ElasticsearchAnalyzerDefinitionTranslator translator);

	@Override
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		try {
			return super.unwrap( analyzerType );
		}
		catch (ClassCastException e) {
			if ( !ElasticsearchAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
				// Not even the same technology, probably a user error
				throw log.invalidConversionFromElasticsearchAnalyzer( this, e );
			}
			else {
				// The other type uses the same technology... probably a bug?
				throw e;
			}
		}
	}

	@Override
	public void close() {
		// Nothing to close in Elasticsearch analyzer references
	}

}
