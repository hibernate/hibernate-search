/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common.spi;

import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;

public final class HibernateOrmSpiUtils {

	private HibernateOrmSpiUtils() {
	}

	@SuppressForbiddenApis(reason = "Safer wrapper")
	public static <T extends Service> T serviceOrFail(ServiceRegistry serviceRegistry, Class<T> serviceClass) {
		return HibernateOrmUtils.getServiceOrFail( serviceRegistry, serviceClass );
	}

}
