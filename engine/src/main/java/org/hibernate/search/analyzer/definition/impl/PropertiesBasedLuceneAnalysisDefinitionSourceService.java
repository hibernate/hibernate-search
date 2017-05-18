/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import java.util.Properties;

import org.hibernate.search.analyzer.definition.LuceneAnalysisDefinitionProvider;
import org.hibernate.search.analyzer.definition.spi.LuceneAnalysisDefinitionSourceService;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * The default LuceneAnalyzerDefinitionSourceService.
 *
 * This service allows to set the {@link Environment#ANALYSIS_DEFINITION_PROVIDER configuration properties} to
 * point to a custom implementation of {@link org.hibernate.search.analyzer.definition.LuceneAnalysisDefinitionProvider}.
 *
 * Integrators which prefer to inject an alternative service by reference rather than setting a configuration
 * property can provide an alternative Service implementation by overriding {@link org.hibernate.search.cfg.spi.SearchConfiguration#getProvidedServices}.
 *
 * @author Sanne Grinovero
 */
public class PropertiesBasedLuceneAnalysisDefinitionSourceService implements LuceneAnalysisDefinitionSourceService, Startable {

	private static final Log log = LoggerFactory.make();
	private LuceneAnalysisDefinitionProvider provider;

	@Override
	public void start(Properties properties, BuildContext context) {
		String providerClassName = properties.getProperty( Environment.ANALYSIS_DEFINITION_PROVIDER );
		if ( providerClassName != null ) {
			try {
				Class<?> providerClazz = ClassLoaderHelper.classForName( providerClassName, context.getServiceManager() );
				provider = (LuceneAnalysisDefinitionProvider) ReflectionHelper.createInstance( providerClazz, true );
			}
			catch (RuntimeException e) {
				throw log.invalidLuceneAnalyzerDefinitionProvider( providerClassName, e );
			}
		}
	}

	@Override
	public LuceneAnalysisDefinitionProvider getLuceneAnalyzerDefinitionProvider() {
		return provider;
	}

}
