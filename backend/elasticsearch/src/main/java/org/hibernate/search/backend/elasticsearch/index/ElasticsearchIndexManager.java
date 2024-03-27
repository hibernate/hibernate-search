/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.index;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.engine.backend.index.IndexManager;

public interface ElasticsearchIndexManager extends IndexManager {

	@Override
	ElasticsearchBackend backend();

	@Override
	ElasticsearchIndexDescriptor descriptor();

}
