/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;

import org.hibernate.search.analyzer.impl.LuceneEmbeddedAnalyzerStrategy;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.nulls.impl.LuceneMissingValueStrategy;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.LuceneEmbeddedIndexFamily;
import org.hibernate.search.indexes.spi.IndexFamilyImplementor;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.util.Version;

public class LuceneEmbeddedIndexFamilyImpl implements IndexFamilyImplementor, LuceneEmbeddedIndexFamily {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final ServiceManager serviceManager;
	private final String defaultAnalyzer;
	private final Version luceneMatchVersion;

	public LuceneEmbeddedIndexFamilyImpl(ServiceManager serviceManager, SearchConfiguration cfg) {
		this.serviceManager = serviceManager;
		this.defaultAnalyzer = cfg.getProperty( Environment.ANALYZER_CLASS );
		this.luceneMatchVersion = getLuceneMatchVersion( cfg );
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> T unwrap(Class<T> unwrappedClass) {
		if ( unwrappedClass.isAssignableFrom( LuceneEmbeddedIndexFamily.class ) ) {
			return (T) this;
		}
		else {
			throw new SearchException( "Cannot unwrap a '" + getClass().getName() + "' into a '" + unwrappedClass.getName() + "'" );
		}
	}

	@Override
	public AnalyzerStrategy createAnalyzerStrategy() {
		return new LuceneEmbeddedAnalyzerStrategy( serviceManager, defaultAnalyzer, luceneMatchVersion );
	}

	@Override
	public MissingValueStrategy getMissingValueStrategy() {
		return LuceneMissingValueStrategy.INSTANCE;
	}

	private Version getLuceneMatchVersion(SearchConfiguration cfg) {
		final Version version;
		String tmp = cfg.getProperty( Environment.LUCENE_MATCH_VERSION );
		if ( StringHelper.isEmpty( tmp ) ) {
			log.recommendConfiguringLuceneVersion();
			version = Environment.DEFAULT_LUCENE_MATCH_VERSION;
		}
		else {
			try {
				version = Version.parseLeniently( tmp );
				if ( log.isDebugEnabled() ) {
					log.debug( "Setting Lucene compatibility to Version " + version );
				}
			}
			catch (IllegalArgumentException e) {
				throw log.illegalLuceneVersionFormat( tmp, e.getMessage() );
			}
			catch (ParseException e) {
				throw log.illegalLuceneVersionFormat( tmp, e.getMessage() );
			}
		}
		return version;
	}
}
