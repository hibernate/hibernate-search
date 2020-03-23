/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.index.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderProvider;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegatorImpl;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.index.DirectoryReader;
import org.easymock.EasyMockSupport;

public class IndexAccessorTest extends EasyMockSupport {

	private static final String INDEX_NAME = "SomeIndexName";

	private EventContext indexEventContext = EventContexts.fromIndexName( INDEX_NAME );
	private DirectoryHolder directoryHolderMock = createStrictMock( DirectoryHolder.class );
	private IndexReaderProvider indexReaderProviderMock = createStrictMock( IndexReaderProvider.class );
	private IndexWriterProvider indexWriterProviderMock = createStrictMock( IndexWriterProvider.class );
	private IndexWriterDelegatorImpl indexWriterDelegatorMock = createStrictMock( IndexWriterDelegatorImpl.class );
	private DirectoryReader indexReaderMock = createStrictMock( DirectoryReader.class );

	private IndexAccessorImpl accessor = new IndexAccessorImpl(
			indexEventContext, directoryHolderMock,
			indexWriterProviderMock, indexReaderProviderMock
	);

	@Before
	public void start() throws IOException {
		resetAll();
		directoryHolderMock.start();
		replayAll();
		accessor.start();
		verifyAll();
	}

	@After
	public void close() throws IOException {
		resetAll();
		indexReaderProviderMock.clear();
		indexWriterProviderMock.clear();
		directoryHolderMock.close();
		replayAll();
		accessor.close();
		verifyAll();
	}

	@Test
	public void commit() throws IOException {
		resetAll();
		expect( indexWriterProviderMock.getOrNull() ).andReturn( indexWriterDelegatorMock );
		indexWriterDelegatorMock.commit();
		replayAll();
		accessor.commit();
		verifyAll();
	}

	@Test
	public void commit_noWriter() {
		resetAll();
		// No writer => nothing to commit
		expect( indexWriterProviderMock.getOrNull() ).andReturn( null );
		replayAll();
		accessor.commit();
		verifyAll();
	}

	@Test
	public void commit_ioException() throws IOException {
		IOException exception = new IOException( "Some message" );

		resetAll();
		expect( indexWriterProviderMock.getOrNull() ).andReturn( indexWriterDelegatorMock );
		indexWriterDelegatorMock.commit();
		expectLastCall().andThrow( exception );
		replayAll();
		SubTest.expectException( () -> accessor.commit() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to commit", indexEventContext.render() )
				.hasCause( exception );
		verifyAll();
	}

	@Test
	public void commit_runtimeException() throws IOException {
		RuntimeException exception = new RuntimeException( "Some message" );

		resetAll();
		expect( indexWriterProviderMock.getOrNull() ).andReturn( indexWriterDelegatorMock );
		indexWriterDelegatorMock.commit();
		expectLastCall().andThrow( exception );
		replayAll();
		SubTest.expectException( () -> accessor.commit() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to commit", indexEventContext.render() )
				.hasCause( exception );
		verifyAll();
	}

	@Test
	public void commitOrDelay_noDelay() throws IOException {
		resetAll();
		expect( indexWriterProviderMock.getOrNull() ).andReturn( indexWriterDelegatorMock );
		expect( indexWriterDelegatorMock.commitOrDelay() ).andReturn( 0L );
		replayAll();
		assertThat( accessor.commitOrDelay() ).isEqualTo( 0L );
		verifyAll();
	}

	@Test
	public void commitOrDelay_delay() throws IOException {
		resetAll();
		expect( indexWriterProviderMock.getOrNull() ).andReturn( indexWriterDelegatorMock );
		expect( indexWriterDelegatorMock.commitOrDelay() ).andReturn( 4242L );
		replayAll();
		assertThat( accessor.commitOrDelay() ).isEqualTo( 4242L );
		verifyAll();
	}

	@Test
	public void commitOrDelay_noWriter() {
		resetAll();
		// No writer => nothing to commit
		expect( indexWriterProviderMock.getOrNull() ).andReturn( null );
		replayAll();
		assertThat( accessor.commitOrDelay() ).isEqualTo( 0L );
		verifyAll();
	}

	@Test
	public void commitOrDelay_ioException() throws IOException {
		IOException exception = new IOException( "Some message" );

		resetAll();
		expect( indexWriterProviderMock.getOrNull() ).andReturn( indexWriterDelegatorMock );
		indexWriterDelegatorMock.commitOrDelay();
		expectLastCall().andThrow( exception );
		replayAll();
		SubTest.expectException( () -> accessor.commitOrDelay() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to commit", indexEventContext.render() )
				.hasCause( exception );
		verifyAll();
	}

	@Test
	public void commitOrDelay_runtimeException() throws IOException {
		RuntimeException exception = new RuntimeException( "Some message" );

		resetAll();
		expect( indexWriterProviderMock.getOrNull() ).andReturn( indexWriterDelegatorMock );
		indexWriterDelegatorMock.commitOrDelay();
		expectLastCall().andThrow( exception );
		replayAll();
		SubTest.expectException( () -> accessor.commitOrDelay() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to commit", indexEventContext.render() )
				.hasCause( exception );
		verifyAll();
	}

	@Test
	public void refresh() throws IOException {
		resetAll();
		indexReaderProviderMock.refresh();
		replayAll();
		accessor.refresh();
		verifyAll();
	}

	@Test
	public void refresh_ioException() throws IOException {
		IOException exception = new IOException( "Some message" );

		resetAll();
		indexReaderProviderMock.refresh();
		expectLastCall().andThrow( exception );
		replayAll();
		SubTest.expectException( () -> accessor.refresh() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to refresh", indexEventContext.render() )
				.hasCause( exception );
		verifyAll();
	}

	@Test
	public void refresh_runtimeException() throws IOException {
		RuntimeException exception = new RuntimeException( "Some message" );

		resetAll();
		indexReaderProviderMock.refresh();
		expectLastCall().andThrow( exception );
		replayAll();
		SubTest.expectException( () -> accessor.refresh() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to refresh", indexEventContext.render() )
				.hasCause( exception );
		verifyAll();
	}

	@Test
	public void mergeSegments() throws IOException {
		resetAll();
		expect( indexWriterProviderMock.getOrCreate() ).andReturn( indexWriterDelegatorMock );
		indexWriterDelegatorMock.mergeSegments();
		replayAll();
		accessor.mergeSegments();
		verifyAll();
	}

	@Test
	public void getIndexWriterDelegator() throws IOException {
		resetAll();
		expect( indexWriterProviderMock.getOrCreate() ).andReturn( indexWriterDelegatorMock );
		replayAll();
		assertThat( accessor.getIndexWriterDelegator() ).isSameAs( indexWriterDelegatorMock );
		verifyAll();
	}

	@Test
	public void getIndexReader() throws IOException {
		resetAll();
		expect( indexReaderProviderMock.getOrCreate() ).andReturn( indexReaderMock );
		replayAll();
		assertThat( accessor.getIndexReader() ).isSameAs( indexReaderMock );
		verifyAll();
	}

}