/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.spi;

import org.hibernate.service.Service;

/**
 * Provides a way to resolve references to externally defined beans.
 * <p>
 * Used in some cases when beans have to be plugged in Hibernate
 * (for instance in Hibernate Search for field bridges and class bridges).
 *
 * @hsearch.experimental This type is under active development.
 *    You should be prepared for incompatible changes in future releases.
 * @since 5.8
 */
public interface BeanResolver extends Service {

	<T> T resolve(Class<?> reference, Class<T> expectedClass);

}
