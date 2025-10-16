/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.spi;

import java.util.Optional;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ElasticsearchRequestInterceptorProvider {

	Optional<ElasticsearchRequestInterceptor> provide(ElasticsearchRequestInterceptorProviderContext context);

}
