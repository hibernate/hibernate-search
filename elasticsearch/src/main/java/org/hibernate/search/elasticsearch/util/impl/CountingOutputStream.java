/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class CountingOutputStream extends FilterOutputStream {

	private long bytesWritten = 0l;

	public CountingOutputStream(OutputStream out) {
		super( out );
	}

	@Override
	public void write(int b) throws IOException {
		out.write( b );
		count( 1 );
	}

	@Override
	public void write(byte[] b) throws IOException {
		write( b, 0, b.length );
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write( b, off, len );
		count( len );
	}

	protected void count(int written) {
		if ( written > 0 ) {
			bytesWritten += written;
		}
	}

	public long getBytesWritten() {
		return bytesWritten;
	}

}
