/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.document.model.spi.IndexField;

public interface ElasticsearchIndexField
		extends IndexField<ElasticsearchSearchIndexScope<?>, ElasticsearchIndexCompositeNode>, ElasticsearchIndexNode {
}
