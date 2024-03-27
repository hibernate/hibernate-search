/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;

public interface IndexSchemaRootContributor {

	void contribute(RootTypeMapping rootTypeMapping);

}
