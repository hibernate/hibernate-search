/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchScroll;

public interface ElasticsearchSearchScroll<H>
		extends ExtendedSearchScroll<H, ElasticsearchSearchScrollResult<H>> {

}
