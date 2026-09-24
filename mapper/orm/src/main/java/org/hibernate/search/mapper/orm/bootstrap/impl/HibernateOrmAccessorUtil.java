/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import org.hibernate.accessor.bytebuddy.ByteBuddyAccessorConfiguration;
import org.hibernate.accessor.spi.AccessorConfiguration;

@SuppressWarnings("unused")
class HibernateOrmAccessorUtil {
	@SuppressWarnings("unused")
	private HibernateOrmAccessorUtil() {
		// NOTE: we do not need this, but to make moditect pick up the dependency
		//  and correctly add it to the module info ... we should reference some class form the dependency...
		AccessorConfiguration empty = ByteBuddyAccessorConfiguration.EMPTY;
	}
}
