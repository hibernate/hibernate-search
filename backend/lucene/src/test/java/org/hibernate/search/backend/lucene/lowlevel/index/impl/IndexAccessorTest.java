/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.index.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderProvider;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegatorImpl;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class IndexAccessorTest {

	private static final String INDEX_NAME = "SomeIndexName";

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private final EventContext indexEventContext = EventContexts.fromIndexName( INDEX_NAME );

	@Mock
	private DirectoryHolder directoryHolderMock;
	@Mock
	private Directory directoryMock;
	@Mock
	private IndexReaderProvider indexReaderProviderMock;
	@Mock
	private IndexWriterProvider indexWriterProviderMock;
	@Mock
	private IndexWriterDelegatorImpl indexWriterDelegatorMock;
	@Mock
	private DirectoryReader indexReaderMock;

	private IndexAccessorImpl accessor;

	@Before
	public void start() throws IOException {
		accessor = new IndexAccessorImpl( indexEventContext, directoryHolderMock,
				indexWriterProviderMock, indexReaderProviderMock );
	}

	@After
	public void close() throws IOException {
		accessor.close();
	}

	@Test
	public void commit() {
		when( indexWriterProviderMock.getOrNull() ).thenReturn( indexWriterDelegatorMock );

		accessor.commit();

		verify( indexWriterDelegatorMock ).commit();
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void commit_noWriter() {
		when( indexWriterProviderMock.getOrNull() ).thenReturn( null );

		accessor.commit();

		// No writer => nothing to commit
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void commit_runtimeException() {
		RuntimeException exception = new RuntimeException( "Some message" );

		when( indexWriterProviderMock.getOrNull() ).thenReturn( indexWriterDelegatorMock );
		doThrow( exception ).when( indexWriterDelegatorMock ).commit();

		assertThatThrownBy( () -> accessor.commit() )
				.isSameAs( exception );
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void commitOrDelay() {
		when( indexWriterProviderMock.getOrNull() ).thenReturn( indexWriterDelegatorMock );

		accessor.commitOrDelay();

		verify( indexWriterDelegatorMock ).commitOrDelay();
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void commitOrDelay_noWriter() {
		when( indexWriterProviderMock.getOrNull() ).thenReturn( null );

		accessor.commitOrDelay();

		// No writer => nothing to commit
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void commitOrDelay_runtimeException() {
		RuntimeException exception = new RuntimeException( "Some message" );

		when( indexWriterProviderMock.getOrNull() ).thenReturn( indexWriterDelegatorMock );
		doThrow( exception ).when( indexWriterDelegatorMock ).commitOrDelay();

		assertThatThrownBy( () -> accessor.commitOrDelay() )
				.isSameAs( exception );
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void refresh() {
		accessor.refresh();

		verify( indexReaderProviderMock ).refresh();
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void refresh_runtimeException() {
		RuntimeException exception = new RuntimeException( "Some message" );

		doThrow( exception ).when( indexReaderProviderMock ).refresh();

		assertThatThrownBy( () -> accessor.refresh() )
				.isSameAs( exception );
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void mergeSegments() throws IOException {
		when( indexWriterProviderMock.getOrCreate() ).thenReturn( indexWriterDelegatorMock );

		accessor.mergeSegments();

		verify( indexWriterDelegatorMock ).mergeSegments();
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void getIndexWriterDelegator() throws IOException {
		when( indexWriterProviderMock.getOrCreate() ).thenReturn( indexWriterDelegatorMock );

		assertThat( accessor.getIndexWriterDelegator() ).isSameAs( indexWriterDelegatorMock );
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void getIndexReader() throws IOException {
		when( indexReaderProviderMock.getOrCreate() ).thenReturn( indexReaderMock );

		assertThat( accessor.getIndexReader() ).isSameAs( indexReaderMock );
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void cleanUpAfterFailure() throws IOException {
		Throwable exception = new RuntimeException( "Some message" );
		Object failingOperation = "Some operation description";

		accessor.cleanUpAfterFailure( exception, failingOperation );

		verify( indexWriterProviderMock ).clearAfterFailure( exception, failingOperation );
		verify( indexReaderProviderMock ).clear();
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void cleanUpAfterFailure_closeFailure() {
		Throwable exception = new RuntimeException( "Some message" );
		Object failingOperation = "Some operation description";
		RuntimeException closeException = new RuntimeException( "Some other message" );

		doThrow( closeException ).when( indexWriterProviderMock ).clearAfterFailure( exception, failingOperation );

		accessor.cleanUpAfterFailure( exception, failingOperation );

		assertThat( exception ).hasSuppressedException( closeException );
		verifyNoOtherIndexInteractions();
	}

	@Test
	public void ccomputeSizeInBytes() throws IOException {
		when( directoryHolderMock.get() ).thenReturn( directoryMock );
		when( directoryMock.listAll() )
				.thenReturn( new String[] { "file0", "file1", "file2", "file3.cfr", "file4.dv" } );
		when( directoryMock.fileLength( "file0" ) ).thenReturn( 32L );
		when( directoryMock.fileLength( "file1" ) ).thenReturn( 489L );
		when( directoryMock.fileLength( "file2" ) ).thenReturn( 8654L );
		when( directoryMock.fileLength( "file3.cfr" ) ).thenReturn( 0L );
		when( directoryMock.fileLength( "file4.dv" ) ).thenReturn( 42L );

		assertThat( accessor.computeSizeInBytes() ).isEqualTo( 9217L );

		verifyNoOtherIndexInteractions();
	}

	@Test
	public void computeSizeInBytes_concurrentDeletion() throws IOException {
		when( directoryHolderMock.get() ).thenReturn( directoryMock );
		when( directoryMock.listAll() )
				.thenReturn( new String[] { "file0", "file1NoSuchFile", "file2", "file3FileNotFound.cfr", "file4.dv" } );
		when( directoryMock.fileLength( "file0" ) ).thenReturn( 32L );
		when( directoryMock.fileLength( "file1NoSuchFile" ) )
				.thenThrow( new NoSuchFileException( "should be ignored" ) );
		when( directoryMock.fileLength( "file2" ) ).thenReturn( 8654L );
		when( directoryMock.fileLength( "file3FileNotFound.cfr" ) )
				.thenThrow( new FileNotFoundException( "should be ignored" ) );
		when( directoryMock.fileLength( "file4.dv" ) ).thenReturn( 42L );

		assertThat( accessor.computeSizeInBytes() ).isEqualTo( 8728L );

		verifyNoOtherIndexInteractions();
	}

	private void verifyNoOtherIndexInteractions() {
		verifyNoMoreInteractions( directoryHolderMock, directoryMock,
				indexWriterProviderMock, indexWriterDelegatorMock,
				indexReaderProviderMock, indexReaderMock );
	}

}
