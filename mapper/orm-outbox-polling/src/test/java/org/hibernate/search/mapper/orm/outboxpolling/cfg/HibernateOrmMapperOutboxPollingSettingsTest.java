/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
