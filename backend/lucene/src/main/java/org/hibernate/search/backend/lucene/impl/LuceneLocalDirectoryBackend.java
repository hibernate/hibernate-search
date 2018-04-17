/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.index.impl.LuceneLocalDirectoryIndexManagerBuilder;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneQueryWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.StubLuceneQueryWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class LuceneLocalDirectoryBackend implements LuceneBackend {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final Path rootDirectory;

	private final LuceneWorkFactory workFactory;

	private final LuceneQueryWorkOrchestrator queryOrchestrator;

	private final MultiTenancyStrategy multiTenancyStrategy;

	public LuceneLocalDirectoryBackend(String name, Path rootDirectory, LuceneWorkFactory workFactory, MultiTenancyStrategy multiTenancyStrategy) {
		this.name = name;
		this.rootDirectory = rootDirectory;

		this.workFactory = workFactory;

		this.queryOrchestrator = new StubLuceneQueryWorkOrchestrator();

		this.multiTenancyStrategy = multiTenancyStrategy;

		initializeRootDirectory( name, rootDirectory );
	}

	@Override
	public String normalizeIndexName(String rawIndexName) {
		return rawIndexName;
	}

	@Override
	public IndexManagerBuilder<LuceneRootDocumentBuilder> createIndexManagerBuilder(
			String indexName, boolean multiTenancyEnabled, BuildContext context, ConfigurationPropertySource propertySource) {
		if ( multiTenancyEnabled && !multiTenancyStrategy.isMultiTenancySupported() ) {
			throw log.multiTenancyRequiredButNotSupportedByBackend( this, indexName );
		}

		return new LuceneLocalDirectoryIndexManagerBuilder( this, normalizeIndexName( indexName ), context, propertySource );
	}

	public Path getRootDirectory() {
		return rootDirectory;
	}

	@Override
	public LuceneWorkFactory getWorkFactory() {
		return workFactory;
	}

	@Override
	public LuceneQueryWorkOrchestrator getQueryOrchestrator() {
		return queryOrchestrator;
	}

	@Override
	public MultiTenancyStrategy getMultiTenancyStrategy() {
		return multiTenancyStrategy;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( queryOrchestrator::close );
		}
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( name ).append( ", " )
				.append( "rootDirectory=" ).append( rootDirectory )
				.append( "]" )
				.toString();
	}

	private static void initializeRootDirectory(String name, Path rootDirectory) {
		if ( Files.exists( rootDirectory ) ) {
			if ( !Files.isDirectory( rootDirectory ) || !Files.isWritable( rootDirectory ) ) {
				throw log.localDirectoryBackendRootDirectoryNotWritableDirectory( name, rootDirectory );
			}
		}
		else {
			try {
				Files.createDirectories( rootDirectory );
			}
			catch (Exception e) {
				throw log.unableToCreateRootDirectoryForLocalDirectoryBackend( name, rootDirectory, e );
			}
		}
	}
}
