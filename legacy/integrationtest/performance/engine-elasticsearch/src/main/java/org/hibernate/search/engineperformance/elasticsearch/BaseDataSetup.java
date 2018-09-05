/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;

import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.model.AbstractBookEntity;
import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity1;
import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity2;
import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity3;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.DatasetCreation;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.SearchIntegratorHelper;
import org.hibernate.search.spi.SearchIntegrator;

public class BaseDataSetup {

	/**
	 * Set this system property to an alternative path if you want
	 * cache files (downloads) to be stored in a specific place
	 * (other than "/tmp/" + TEST_DIR_PREFIX + "cache").
	 */
	private static final String CACHE_PROPERTY = "cache-path";

	/**
	 * Prefix used to identify the generated temporary directories for
	 * running tests which need writing to a filesystem.
	 */
	private static final String TEST_DIR_PREFIX = "HibernateSearch-Perftests-";

	private final List<Dataset<? extends AbstractBookEntity>> datasets = new ArrayList<>();

	protected void initializeState(String datasetName) throws IOException, URISyntaxException {
		datasets.add( DatasetCreation.createDataset( BookEntity1::new, BookEntity1.TYPE_ID, datasetName, pickCacheDirectory() ) );
		datasets.add( DatasetCreation.createDataset( BookEntity2::new, BookEntity2.TYPE_ID, datasetName, pickCacheDirectory() ) );
		datasets.add( DatasetCreation.createDataset( BookEntity3::new, BookEntity3.TYPE_ID, datasetName, pickCacheDirectory() ) );
	}

	protected void initializeIndexes(SearchIntegrator si, int indexSize) {
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		List<ForkJoinTask<?>> tasks = new ArrayList<>();
		for ( Dataset<? extends AbstractBookEntity> dataset : datasets ) {
			ForkJoinTask<?> task = forkJoinPool.submit(
					() -> SearchIntegratorHelper.preindexEntities( si, dataset, IntStream.range( 0, indexSize ) )
			);
			tasks.add( task );
		}
		for ( ForkJoinTask<?> task : tasks ) {
			task.join();
		}
	}

	public Dataset<? extends AbstractBookEntity> getDataset(int threadIndex) {
		return datasets.get( threadIndex % datasets.size() );
	}

	protected Path pickCacheDirectory() throws IOException {
		String userSelectedPath = System.getProperty( CACHE_PROPERTY );
		Path path;
		if ( userSelectedPath != null ) {
			path = Paths.get( userSelectedPath );
		}
		else {
			path = Paths.get( System.getProperty( "java.io.tmpdir" ) )
					.resolve( TEST_DIR_PREFIX + "cache" );
		}
		if ( ! Files.exists( path ) ) {
			Files.createDirectory( path );
		}
		return path;
	}

}
