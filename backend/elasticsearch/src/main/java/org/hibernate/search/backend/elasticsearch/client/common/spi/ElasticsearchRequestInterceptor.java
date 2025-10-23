/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.spi;

import java.io.IOException;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ElasticsearchRequestInterceptor {

	void intercept(ElasticsearchRequestInterceptorContext requestContext) throws IOException;

}
