/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.spi;

import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.search.mapper.orm.bootstrap.impl.HibernateOrmIntegrationBooterImpl;

public interface HibernateOrmIntegrationBooter {

	static HibernateOrmIntegrationBooter create(Metadata metadata, BootstrapContext bootstrapContext) {
		return new HibernateOrmIntegrationBooterImpl( metadata, bootstrapContext );
	}

	void preBoot(BiConsumer<String, Object> propertyCollector);

}
