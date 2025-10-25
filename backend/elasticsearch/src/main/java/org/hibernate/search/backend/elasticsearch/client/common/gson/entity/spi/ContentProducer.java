/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.backend.elasticsearch.client.common.gson.entity.spi;

import java.io.Closeable;
import java.io.IOException;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ContentProducer extends Closeable {

	void produceContent(ContentEncoder encoder) throws IOException;

}
