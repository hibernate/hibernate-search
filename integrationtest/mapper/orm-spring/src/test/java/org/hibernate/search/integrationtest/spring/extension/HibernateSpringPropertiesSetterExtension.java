/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.extension;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class HibernateSpringPropertiesSetterExtension implements BeforeAllCallback {
	@Override
	public void beforeAll(ExtensionContext context) {
		DatabaseContainer.springConfiguration();
	}
}
