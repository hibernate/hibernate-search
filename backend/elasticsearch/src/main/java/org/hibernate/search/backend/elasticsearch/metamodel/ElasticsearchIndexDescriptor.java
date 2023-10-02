/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.metamodel;

import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;

/**
 * A descriptor of an Elasticsearch backend index,
 * which exposes additional information specific to this backend.
 */
public interface ElasticsearchIndexDescriptor extends IndexDescriptor {

	/**
	 * @return The read name,
	 * i.e. the name that Hibernate Search is supposed to use when executing searches on the index.
	 */
	String readName();

	/**
	 * @return The write name,
	 * i.e. the name that Hibernate Search is supposed to use when indexing or purging the index.
	 */
	String writeName();

}
