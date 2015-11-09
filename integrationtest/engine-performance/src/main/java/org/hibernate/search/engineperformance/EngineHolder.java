/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hibernate.search.engineperformance.model.BookEntity;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.util.impl.FileHelper;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Prepares the Search Engine to be tested: applies configuration options
 * and takes care of cleanup at shutdown.
 */
@State(Scope.Benchmark)
public class EngineHolder {

	/**
	 * Set this system property to an alternative path if you don't
	 * want the filesystem based performance tests to be run on your
	 * default temp path.
	 */
	private static final String INDEX_PATH_PROPERTY = "index-path";

	/**
	 * Prefix used to identify the generated temporary directories for
	 * running tests which need writing to a filesystem.
	 */
	private static final String TEST_DIR_PREFIX = "HibernateSearch-Perftests-";

	public volatile SearchIntegrator si;

	@Param( { "ram","fs", "blackhole" } )
	private String backend;

	//Instance variable to allow cleanup after benchmark execution
	private Path createdTempDirectory;

	@Setup
	public void initializeState() throws IOException {
		pickIndexStorageDirectory();
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		switch (backend) {
			case "ram" :
				cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
				break;
			case "fs" :
				cfg.addProperty( "hibernate.search.default.directory_provider", "filesystem" );
				break;
			case "fs-nrt" :
				cfg.addProperty( "hibernate.search.default.directory_provider", "filesystem" );
				cfg.addProperty( "hibernate.search.default.indexmanager", "near-real-time" );
				break;
			case "blackhole" :
				cfg.addProperty( "hibernate.search.default.worker.backend", "blackhole" );
				break;
			default :
				throw new RuntimeException( "Parameter 'backend'='" + backend + "' not recognized!" );
		}
		cfg.addProperty( "hibernate.search.default.indexBase", createdTempDirectory.toString() );
		cfg.addClass( BookEntity.class );
		SearchIntegrator searchIntegrator = new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
		si = searchIntegrator;
	}

	private void pickIndexStorageDirectory() throws IOException {
		String userSelectedPath = System.getProperty( INDEX_PATH_PROPERTY );
		if ( userSelectedPath != null ) {
			Path pathPrefix = Paths.get( userSelectedPath );
			createdTempDirectory = Files.createTempDirectory( pathPrefix, TEST_DIR_PREFIX );
		}
		else {
			createdTempDirectory = Files.createTempDirectory( TEST_DIR_PREFIX );
		}
	}

	@TearDown
	public void shutdownState() throws IOException {
		final SearchIntegrator integrator = si;
		if ( integrator != null ) {
			integrator.close();
		}
		FileHelper.delete( createdTempDirectory );
	}

}
