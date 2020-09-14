/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.service;

import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;

/**
 * @author Hardy Ferentschik
 */
public interface FooService extends Service, Startable {
}
