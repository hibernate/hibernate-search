/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.support;

import org.junit.rules.ExternalResource;

import com.arjuna.ats.jta.utils.JNDIManager;
import org.jnp.server.NamingBeanImpl;

public class JtaEnvironment extends ExternalResource {

	private NamingBeanImpl NAMING_BEAN;

	@Override
	protected void before() throws Throwable {
		NAMING_BEAN = new NamingBeanImpl();
		NAMING_BEAN.start();

		JNDIManager.bindJTAImplementation();
	}

	@Override
	protected void after() {
		NAMING_BEAN.stop();
	}
}
