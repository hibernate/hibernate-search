/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.setuputilities;

import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientFactory;
import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity;
import org.hibernate.search.engineperformance.elasticsearch.stub.BlackholeElasticsearchClientFactory;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;

public class SearchIntegratorCreation {

	private SearchIntegratorCreation() {
		//do not construct
	}

	public static SearchIntegrator createIntegrator(String client, ConnectionInfo connectionInfo, boolean refreshAfterWrite, String workerExecution) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();

		cfg.addProperty( "hibernate.search.default.indexmanager", "elasticsearch" );
		cfg.addProperty( "hibernate.search.default.elasticsearch.required_index_status", "yellow" );
		cfg.addProperty( "hibernate.search.default.elasticsearch.index_schema_management_strategy", "drop-and-create-and-drop" );
		cfg.addProperty( "hibernate.search.default.elasticsearch.refresh_after_write", String.valueOf( refreshAfterWrite ) );
		addIfNonNull( cfg, "hibernate.search.default.worker.execution", workerExecution );

		if ( "default".equals( client ) ) {
			cfg.addProperty( "hibernate.search.default.elasticsearch.host", connectionInfo.getHost() );
			addIfNonNull( cfg, "hibernate.search.default.elasticsearch.username", connectionInfo.getUsername() );
			addIfNonNull( cfg, "hibernate.search.default.elasticsearch.password", connectionInfo.getPassword() );
			addIfNonNull( cfg, "hibernate.search.default.elasticsearch.aws.access_key", connectionInfo.getAwsAccessKey() );
			addIfNonNull( cfg, "hibernate.search.default.elasticsearch.aws.secret_key", connectionInfo.getAwsSecretKey() );
			addIfNonNull( cfg, "hibernate.search.default.elasticsearch.aws.region", connectionInfo.getAwsRegion() );
		}
		else if ( client.startsWith( "blackhole-" ) ) {
			String elasticsearchVersion = client.replaceFirst( "^blackhole-", "" );
			cfg.addProvidedService( ElasticsearchClientFactory.class, new BlackholeElasticsearchClientFactory( elasticsearchVersion ) );
		}
		else {
			throw new IllegalArgumentException( "Illegal value for parameter 'client': '" + client + "'." );
		}

		cfg.addClass( BookEntity.class );

		return new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
	}

	public static void preindexEntities(SearchIntegrator si, Dataset data, IntStream idStream) {
		println( "Starting entity pre-indexing..." );
		Indexer indexer = new Indexer( si, data );
		idStream.forEach( indexer );
		indexer.flush();
		println( " ... added " + indexer.count + " entities to the index." );
	}

	private static class Indexer implements IntConsumer {
		private final Worker worker;
		private final Dataset data;
		private TransactionContextForTest tc = new TransactionContextForTest();
		private boolean needsFlush = false;
		private int count = 0;

		public Indexer(SearchIntegrator si, Dataset data) {
			this.worker = si.getWorker();
			this.data = data;
		}

		@Override
		public void accept(int id) {
			BookEntity book = data.create( id );
			Work work = new Work( book, book.getId(), WorkType.ADD, false );
			worker.performWork( work, tc );
			needsFlush = true;
			++count;
			if ( count % 1000 == 0 ) {
				//commit in batches of 1000:
				flush();
			}
		}

		public void flush() {
			if ( needsFlush ) {
				//commit remaining work
				tc.end();
				needsFlush = false;
				tc = new TransactionContextForTest();
			}
		}
	}

	private static void addIfNonNull(SearchConfigurationForTest cfg, String propertyName, String propertyValue) {
		if ( propertyValue != null ) {
			cfg.addProperty( propertyName, propertyValue );
		}
	}

	private static void println(String string) {
		//We strictly disallow System.out usage in the whole project,
		//however this is a CLI tool so allow it here:
		//CHECKSTYLE:OFF
		System.out.println( string );
		//CHECKSTYLE:ON
	}

}
