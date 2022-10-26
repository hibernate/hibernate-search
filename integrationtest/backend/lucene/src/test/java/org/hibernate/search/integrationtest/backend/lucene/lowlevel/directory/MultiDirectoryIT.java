/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerImpl;
import org.hibernate.search.backend.lucene.index.impl.Shard;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.apache.lucene.store.ByteBuffersDirectory;

public class MultiDirectoryIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	protected static final SimpleMappedIndex<IndexBinding> index1 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "index1" );
	protected static final SimpleMappedIndex<IndexBinding> index2 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "index2" );
	protected static final SimpleMappedIndex<IndexBinding> index3 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "index3" );
	protected static final SimpleMappedIndex<IndexBinding> index4 = SimpleMappedIndex.of( IndexBinding::new )
			.name( "index4" );

	@Test
	public void test() throws IOException {
		Path root1Directory = temporaryFolder.getRoot().toPath();
		Path index1Directory = root1Directory.resolve( index1.name() );
		Path index2Directory = root1Directory.resolve( index2.name() );
		Path root2Directory = temporaryFolder.getRoot().toPath();
		Path index3Directory = root2Directory.resolve( index3.name() );

		assertThat( index1Directory ).doesNotExist();
		assertThat( index2Directory ).doesNotExist();
		assertThat( index3Directory ).doesNotExist();

		setupHelper.start()
				.withIndexes( index1, index2, index3, index4 )
				// Default: FS, root 1
				.withBackendProperty( LuceneIndexSettings.DIRECTORY_TYPE, "local-filesystem" )
				.withBackendProperty( LuceneIndexSettings.DIRECTORY_ROOT, root1Directory.toString() )
				// Index 3: FS, root 2
				.withIndexProperty( index3.name(), LuceneIndexSettings.DIRECTORY_ROOT, root2Directory.toString() )
				// Index 4: heap
				.withIndexProperty( index4.name(), LuceneIndexSettings.DIRECTORY_TYPE, "local-heap" )
				.setup();

		assertThat( index1Directory ).exists();
		assertThat( index2Directory ).exists();
		assertThat( index3Directory ).exists();

		LuceneIndexManagerImpl luceneIndexManager = index4.unwrapForTests( LuceneIndexManagerImpl.class );
		assertThat( luceneIndexManager.getShardsForTests() )
				.extracting( Shard::indexAccessorForTests )
				.extracting( IndexAccessorImpl::getDirectoryForTests )
				.isNotEmpty()
				.allSatisfy( directory -> assertThat( directory ).isInstanceOf( ByteBuffersDirectory.class ) );

		long index1ContentSizeBeforeIndexing = directorySize( index1Directory );
		long index2ContentSizeBeforeIndexing = directorySize( index2Directory );
		long index3ContentSizeBeforeIndexing = directorySize( index3Directory );

		checkIndexingAndQuerying( index1 );
		checkIndexingAndQuerying( index2 );
		checkIndexingAndQuerying( index3 );
		checkIndexingAndQuerying( index4 );

		long index1ContentSizeAfterIndexing = directorySize( index1Directory );
		long index2ContentSizeAfterIndexing = directorySize( index2Directory );
		long index3ContentSizeAfterIndexing = directorySize( index3Directory );
		assertThat( index1ContentSizeAfterIndexing )
				.isGreaterThan( index1ContentSizeBeforeIndexing );
		assertThat( index2ContentSizeAfterIndexing )
				.isGreaterThan( index2ContentSizeBeforeIndexing );
		assertThat( index3ContentSizeAfterIndexing )
				.isGreaterThan( index3ContentSizeBeforeIndexing );
	}

	protected final void checkIndexingAndQuerying(SimpleMappedIndex<IndexBinding> index) {
		IndexIndexingPlan plan = index.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( index.binding().string, "text 1" );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( index.binding().string, "text 2" );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( index.binding().string, "text 3" );
		} );
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// Check that all documents are searchable
		assertThatQuery( index.query().where( f -> f.matchAll() ) )
				.hasDocRefHitsAnyOrder(
						index.typeName(),
						DOCUMENT_1, DOCUMENT_2, DOCUMENT_3
				);
	}

	private static long directorySize(Path directory) throws IOException {
		return Files.walk( directory )
				.filter( p -> p.toFile().isFile() )
				.mapToLong( p -> p.toFile().length() )
				.sum();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field(
					"string",
					f -> f.asString()
			)
					.toReference();
		}
	}
}
