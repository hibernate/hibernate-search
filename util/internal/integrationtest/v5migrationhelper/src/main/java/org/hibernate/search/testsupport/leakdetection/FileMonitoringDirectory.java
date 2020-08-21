/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.leakdetection;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMDirectory;

/**
 * This Directory keeps track of opened IndexInput and IndexOutput
 * instances, making it possible to verify if any file was left open.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class FileMonitoringDirectory extends RAMDirectory {

	private final ConcurrentMap<IndexOutput,IndexOutput> openOutputs = new ConcurrentHashMap<IndexOutput, IndexOutput>( 10 );
	private final ConcurrentMap<IndexInput,IndexInput> openInputs = new ConcurrentHashMap<IndexInput, IndexInput>( 40 );

	@Override
	public IndexOutput createOutput(String name, IOContext context) throws IOException {
		IndexOutput indexOutput = super.createOutput( name, context );
		IndexOutputDelegate tracked = new IndexOutputDelegate( indexOutput );
		openOutputs.put( tracked, tracked );
		return tracked;
	}

	@Override
	public IndexInput openInput(String name, IOContext context) throws IOException {
		IndexInput openInput = super.openInput( name, context );
		IndexInputDelegate tracked = new IndexInputDelegate( openInput );
		openInputs.put( tracked, tracked );
		return tracked;
	}

	/**
	 * @return true if this Directory was closed
	 */
	public boolean isClosed() {
		return isOpen == false;
	}

	/**
	 * @return true if all files opened by this Directory were also closed
	 */
	public boolean allFilesWereClosed() {
		return openInputs.isEmpty() && openOutputs.isEmpty();
	}

	private class IndexOutputDelegate extends IndexOutput {

		private final IndexOutput delegate;

		public IndexOutputDelegate(IndexOutput delegate) {
			super( "Testing Delegate: " + delegate.toString() );
			this.delegate = delegate;
		}

		@Override
		public String toString() {
			return "IndexOutputDelegate to " + delegate.toString();
		}

		@Override
		public void close() throws IOException {
			delegate.close();
			openOutputs.remove( IndexOutputDelegate.this );
		}

		// All remaining methods are generated as plain delegators,
		// except equals & hashcode :

		@Override
		public void writeByte(byte b) throws IOException {
			delegate.writeByte( b );
		}

		@Override
		public void writeBytes(byte[] b, int length) throws IOException {
			delegate.writeBytes( b, length );
		}

		@Override
		public long getFilePointer() {
			return delegate.getFilePointer();
		}

		@Override
		public void writeBytes(byte[] b, int offset, int length) throws IOException {
			delegate.writeBytes( b, offset, length );
		}

		@Override
		public void writeInt(int i) throws IOException {
			delegate.writeInt( i );
		}

		@Override
		public void writeShort(short i) throws IOException {
			delegate.writeShort( i );
		}

		@Override
		public void writeLong(long i) throws IOException {
			delegate.writeLong( i );
		}

		@Override
		public void writeString(String s) throws IOException {
			delegate.writeString( s );
		}

		@Override
		public void copyBytes(DataInput input, long numBytes) throws IOException {
			delegate.copyBytes( input, numBytes );
		}

		@Deprecated
		@Override
		public void writeStringStringMap(Map<String, String> map) throws IOException {
			delegate.writeStringStringMap( map );
		}

		@Deprecated
		@Override
		public void writeStringSet(Set<String> set) throws IOException {
			delegate.writeStringSet( set );
		}

		@Override
		public long getChecksum() throws IOException {
			return delegate.getChecksum();
		}

	}

	private class IndexInputDelegate extends IndexInput {

		private final IndexInput delegate;

		public IndexInputDelegate(IndexInput delegate) {
			super( delegate.toString() );
			this.delegate = delegate;
		}

		@Override
		public void close() throws IOException {
			delegate.close();
			openInputs.remove( IndexInputDelegate.this );
		}

		@Override
		public String toString() {
			return "IndexInputDelegate to " + delegate.toString();
		}

		// All remaining methods are generated as plain delegators,
		// except equals & hashcode :

		@Override
		public byte readByte() throws IOException {
			return delegate.readByte();
		}

		@Override
		public void readBytes(byte[] b, int offset, int len) throws IOException {
			delegate.readBytes( b, offset, len );
		}

		@Override
		public void readBytes(byte[] b, int offset, int len, boolean useBuffer) throws IOException {
			delegate.readBytes( b, offset, len, useBuffer );
		}

		@Override
		public long getFilePointer() {
			return delegate.getFilePointer();
		}

		@Override
		public void seek(long pos) throws IOException {
			delegate.seek( pos );
		}

		@Override
		public long length() {
			return delegate.length();
		}

		@Override
		public short readShort() throws IOException {
			return delegate.readShort();
		}

		@Override
		public int readInt() throws IOException {
			return delegate.readInt();
		}

		@Override
		public int readVInt() throws IOException {
			return delegate.readVInt();
		}

		@Override
		public long readLong() throws IOException {
			return delegate.readLong();
		}

		@Override
		public long readVLong() throws IOException {
			return delegate.readVLong();
		}

		@Override
		public String readString() throws IOException {
			return delegate.readString();
		}

		@Deprecated
		@Override
		public Map<String, String> readStringStringMap() throws IOException {
			return delegate.readStringStringMap();
		}

		@Deprecated
		@Override
		public Set<String> readStringSet() throws IOException {
			return delegate.readStringSet();
		}

		@Override
		public IndexInput slice(String sliceDescription, long offset, long length) throws IOException {
			return delegate.slice( sliceDescription, offset, length );
		}

		@Override
		public void skipBytes(final long numBytes) throws IOException {
			delegate.skipBytes( numBytes );
		}

		@Override
		public IndexInput clone() {
			//This is needed to make sure that the cloned instance can seek independently
			IndexInput clonedDelegate = (IndexInput) delegate.clone();
			return new IndexInputDelegate( clonedDelegate );
		}

	}

}
