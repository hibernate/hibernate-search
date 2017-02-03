/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.elasticsearch.settings.impl.ElasticsearchAnalyzerDefinitionTranslator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerStrategy implements AnalyzerStrategy {

	private final ServiceManager serviceManager;

	public ElasticsearchAnalyzerStrategy(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	@Override
	public ElasticsearchAnalyzerReference createDefaultAnalyzerReference() {
		return new NamedElasticsearchAnalyzerReference( "default" );
	}

	@Override
	public ElasticsearchAnalyzerReference createPassThroughAnalyzerReference() {
		return new NamedElasticsearchAnalyzerReference( "keyword" );
	}

	@Override
	public NamedElasticsearchAnalyzerReference createNamedAnalyzerReference(String name) {
		return new NamedElasticsearchAnalyzerReference( name );
	}

	@Override
	public ElasticsearchAnalyzerReference createLuceneClassAnalyzerReference(Class<?> analyzerClass) {
		return new LuceneClassElasticsearchAnalyzerReference( analyzerClass );
	}

	@Override
	public void initializeAnalyzerReferences(Collection<AnalyzerReference> references, Map<String, AnalyzerDef> analyzerDefinitions) {
		try ( ServiceReference<ElasticsearchAnalyzerDefinitionTranslator> translatorReference =
				serviceManager.requestReference( ElasticsearchAnalyzerDefinitionTranslator.class ) ) {
			ElasticsearchAnalyzerDefinitionTranslator translator = translatorReference.get();

			for ( AnalyzerReference reference : references ) {
				if ( reference.is( NamedElasticsearchAnalyzerReference.class ) ) {
					NamedElasticsearchAnalyzerReference namedReference = reference.unwrap( NamedElasticsearchAnalyzerReference.class );
					if ( !namedReference.isInitialized() ) {
						initializeNamedReference( namedReference, analyzerDefinitions );
					}
				}
				else if ( reference.is( LuceneClassElasticsearchAnalyzerReference.class ) ) {
					LuceneClassElasticsearchAnalyzerReference luceneClassReference = reference.unwrap( LuceneClassElasticsearchAnalyzerReference.class );
					if ( !luceneClassReference.isInitialized() ) {
						initializeLuceneClassReference( luceneClassReference, translator );
					}
				}
				else if ( reference.is( ScopedElasticsearchAnalyzerReference.class ) ) {
					ScopedElasticsearchAnalyzerReference scopedReference = reference.unwrap( ScopedElasticsearchAnalyzerReference.class );
					if ( !scopedReference.isInitialized() ) {
						scopedReference.initialize();
					}
				}
			}
		}
	}

	private void initializeNamedReference(NamedElasticsearchAnalyzerReference analyzerReference, Map<String, AnalyzerDef> analyzerDefinitions) {
		String name = analyzerReference.getAnalyzerName();

		ElasticsearchAnalyzer analyzer;
		AnalyzerDef analyzerDefinition = analyzerDefinitions.get( name );
		if ( analyzerDefinition == null ) {
			analyzer = new UndefinedElasticsearchAnalyzerImpl( name );
		}
		else {
			analyzer = new CustomElasticsearchAnalyzerImpl( analyzerDefinition );
		}

		analyzerReference.initialize( analyzer );
	}

	private void initializeLuceneClassReference(LuceneClassElasticsearchAnalyzerReference analyzerReference,
			ElasticsearchAnalyzerDefinitionTranslator translator) {
		Class<?> clazz = analyzerReference.getLuceneClass();

		String name = translator.translate( clazz );

		ElasticsearchAnalyzer analyzer = new UndefinedElasticsearchAnalyzerImpl( name );

		analyzerReference.initialize( name, analyzer );
	}

	@Override
	public ScopedElasticsearchAnalyzerReference.Builder buildScopedAnalyzerReference(AnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedElasticsearchAnalyzerReference.DeferredInitializationBuilder(
				initialGlobalAnalyzerReference.unwrap( ElasticsearchAnalyzerReference.class ),
				Collections.<String, ElasticsearchAnalyzerReference>emptyMap()
				);
	}
}
