/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;

import org.apache.http.nio.IOControl;

public final class StubIOControl implements IOControl {
	public static final StubIOControl INSTANCE = new StubIOControl();

	private StubIOControl() {
	}

	@Override
	public void requestInput() {
		throw unsupported();
	}

	@Override
	public void suspendInput() {
		throw unsupported();
	}

	@Override
	public void requestOutput() {
		throw unsupported();
	}

	@Override
	public void suspendOutput() {
		throw unsupported();
	}

	@Override
	public void shutdown() throws IOException {
		// Nothing to do.
	}

	private UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException( "This IOControl instance is a stub" );
	}
}
