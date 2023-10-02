/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HibernateOrmMapperOutboxPollingSettingsTest {

	@Test
	void coordinationKey() {
		assertThat( HibernateOrmMapperOutboxPollingSettings.coordinationKey( "foo.bar" ) )
				.isEqualTo( "hibernate.search.coordination.foo.bar" );
		assertThat( HibernateOrmMapperOutboxPollingSettings.coordinationKey( "myTenant", "foo.bar" ) )
				.isEqualTo( "hibernate.search.coordination.tenants.myTenant.foo.bar" );
		assertThat( HibernateOrmMapperOutboxPollingSettings.coordinationKey( null, "foo.bar" ) )
				.isEqualTo( "hibernate.search.coordination.foo.bar" );
	}

}
