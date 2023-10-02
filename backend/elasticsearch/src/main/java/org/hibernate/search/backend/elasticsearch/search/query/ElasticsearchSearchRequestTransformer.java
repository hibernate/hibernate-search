/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A pluggable component that gets the chance to transform search requests (path, body, ...)
 * before they are sent to Elasticsearch.
 * <p>
 * <strong>WARNING:</strong> Direct changes to the request may conflict with Hibernate Search features
 * and be supported differently by different versions of Elasticsearch.
 * Thus they cannot be guaranteed to continue to work when upgrading Hibernate Search,
 * even for micro upgrades ({@code x.y.z} to {@code x.y.(z+1)}).
 * Use this at your own risk.
 */
@Incubating
public interface ElasticsearchSearchRequestTransformer {

	void transform(ElasticsearchSearchRequestTransformerContext context);

}
