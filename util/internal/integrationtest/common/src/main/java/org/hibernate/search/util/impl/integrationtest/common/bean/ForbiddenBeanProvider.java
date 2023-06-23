/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.bean;

import static org.assertj.core.api.Assertions.fail;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;

public class ForbiddenBeanProvider implements BeanProvider {

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> BeanHolder<T> forType(Class<T> typeReference) {
		return fail( "Bean request for type '%s' was unexpectedly routed to the bean manager."
				+ " If this test does rely on custom beans, call 'expectCustomBeans()' on the setup helper."
				+ " If this test doesn't rely on custom beans, there is probably a bug in Hibernate Search:"
				+ " look for references to built-in beans that"
				+ " don't use the 'reflection' or 'built-in' provider as they should.",
				typeReference );
	}

	@Override
	public <T> BeanHolder<T> forTypeAndName(Class<T> typeReference, String nameReference) {
		return fail( "Bean request for type '%s' and name '%s' was unexpectedly routed to the bean manager."
				+ " If this test does rely on custom beans, call 'expectCustomBeans()' on the setup helper."
				+ " If this test doesn't rely on custom beans, there is probably a bug in Hibernate Search:"
				+ " look for references to built-in beans that"
				+ " don't use the 'reflection' or 'built-in' provider as they should.",
				typeReference, nameReference );
	}
}
