/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.backend.elasticsearch.client.common.gson.entity.spi;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ContentEncoder {

	int write(ByteBuffer src) throws IOException;

	void complete() throws IOException;

	boolean isCompleted();

	default <T> T unwrap(Class<T> clazz) {
		return clazz.cast( this );
	}

}
