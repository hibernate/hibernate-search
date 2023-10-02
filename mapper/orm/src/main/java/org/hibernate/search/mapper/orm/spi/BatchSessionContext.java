/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.spi;

import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;

public interface BatchSessionContext {

	PojoIndexer createIndexer();

}
