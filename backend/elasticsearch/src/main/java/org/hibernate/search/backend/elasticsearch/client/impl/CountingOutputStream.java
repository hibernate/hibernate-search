/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class CountingOutputStream extends FilterOutputStream {

	private long bytesWritten = 0L;

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

	void count(int written) {
		if ( written > 0 ) {
			bytesWritten += written;
		}
	}

	public long getBytesWritten() {
		return bytesWritten;
	}

}
