/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.util.leakdetection;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMDirectory;

/**
 * This Directory keeps track of opened IndexInput and IndexOutput
 * instances, making it possible to verify if any file was left open.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class FileMonitoringDirectory extends RAMDirectory {

	private final ConcurrentMap<IndexOutput,IndexOutput> openOutputs = new ConcurrentHashMap<IndexOutput, IndexOutput>( 10 );
	private final ConcurrentMap<IndexInput,IndexInput> openInputs = new ConcurrentHashMap<IndexInput, IndexInput>( 40 );

	@Override
	public IndexOutput createOutput(String name) throws IOException {
		IndexOutput indexOutput = super.createOutput( name );
		IndexOutputDelegate tracked = new IndexOutputDelegate( indexOutput );
		openOutputs.put( tracked, tracked );
		return tracked;
	}

	@Override
	public IndexInput openInput(String name) throws IOException {
		IndexInput openInput = super.openInput( name );
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
			this.delegate = delegate;
		}

		public String toString() {
			return "IndexOutputDelegate to " + delegate.toString();
		}

		public void close() throws IOException {
			delegate.close();
			openOutputs.remove( IndexOutputDelegate.this );
		}

		// All remaining methods are generated as plain delegators,
		// except equals & hashcode :

		public void writeByte(byte b) throws IOException {
			delegate.writeByte( b );
		}

		public void flush() throws IOException {
			delegate.flush();
		}

		public void writeBytes(byte[] b, int length) throws IOException {
			delegate.writeBytes( b, length );
		}

		public long getFilePointer() {
			return delegate.getFilePointer();
		}

		public void writeBytes(byte[] b, int offset, int length) throws IOException {
			delegate.writeBytes( b, offset, length );
		}

		public void seek(long pos) throws IOException {
			delegate.seek( pos );
		}

		public long length() throws IOException {
			return delegate.length();
		}

		public void setLength(long length) throws IOException {
			delegate.setLength( length );
		}

		public void writeInt(int i) throws IOException {
			delegate.writeInt( i );
		}

		public void writeShort(short i) throws IOException {
			delegate.writeShort( i );
		}

		public void writeLong(long i) throws IOException {
			delegate.writeLong( i );
		}

		public void writeString(String s) throws IOException {
			delegate.writeString( s );
		}

		public void copyBytes(DataInput input, long numBytes) throws IOException {
			delegate.copyBytes( input, numBytes );
		}

		public void writeChars(String s, int start, int length) throws IOException {
			delegate.writeChars( s, start, length );
		}

		public void writeChars(char[] s, int start, int length) throws IOException {
			delegate.writeChars( s, start, length );
		}

		public void writeStringStringMap(Map<String, String> map) throws IOException {
			delegate.writeStringStringMap( map );
		}

	}

	private class IndexInputDelegate extends IndexInput {

		private final IndexInput delegate;

		public IndexInputDelegate(IndexInput delegate) {
			super( delegate.toString() );
			this.delegate = delegate;
		}

		public void close() throws IOException {
			delegate.close();
			openInputs.remove( IndexInputDelegate.this );
		}

		public String toString() {
			return "IndexInputDelegate to " + delegate.toString();
		}

		// All remaining methods are generated as plain delegators,
		// except equals & hashcode :

		public void skipChars(int length) throws IOException {
			delegate.skipChars( length );
		}

		public void setModifiedUTF8StringsMode() {
			delegate.setModifiedUTF8StringsMode();
		}

		public byte readByte() throws IOException {
			return delegate.readByte();
		}

		public void readBytes(byte[] b, int offset, int len) throws IOException {
			delegate.readBytes( b, offset, len );
		}

		public void readBytes(byte[] b, int offset, int len, boolean useBuffer) throws IOException {
			delegate.readBytes( b, offset, len, useBuffer );
		}

		public short readShort() throws IOException {
			return delegate.readShort();
		}

		public long getFilePointer() {
			return delegate.getFilePointer();
		}

		public void seek(long pos) throws IOException {
			delegate.seek( pos );
		}

		public int readInt() throws IOException {
			return delegate.readInt();
		}

		public long length() {
			return delegate.length();
		}

		public void copyBytes(IndexOutput out, long numBytes) throws IOException {
			delegate.copyBytes( out, numBytes );
		}

		public int readVInt() throws IOException {
			return delegate.readVInt();
		}

		public long readLong() throws IOException {
			return delegate.readLong();
		}

		public long readVLong() throws IOException {
			return delegate.readVLong();
		}

		public String readString() throws IOException {
			return delegate.readString();
		}

		public void readChars(char[] buffer, int start, int length) throws IOException {
			delegate.readChars( buffer, start, length );
		}

		public Map<String, String> readStringStringMap() throws IOException {
			return delegate.readStringStringMap();
		}

	}

}
