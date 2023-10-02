/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
