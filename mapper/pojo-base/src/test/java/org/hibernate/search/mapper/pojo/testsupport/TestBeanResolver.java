/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.testsupport;

import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.util.common.AssertionFailure;

public class TestBeanResolver implements BeanResolver {
	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference, BeanRetrieval retrieval) {
		switch ( retrieval ) {
			case ANY:
			case CONSTRUCTOR:
				try {
					return BeanHolder.of( typeReference.getConstructor().newInstance() );
				}
				catch (Exception e) {
					throw new AssertionFailure( "Exception instantiating " + typeReference, e );
				}
			default:
				throw new AssertionFailure(
						"Retrieval " + retrieval + " is not supported in this test implementation" );
		}
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference, BeanRetrieval retrieval) {
		throw notSupported();
	}

	@Override
	public <T> List<BeanReference<T>> allConfiguredForRole(Class<T> role) {
		throw notSupported();
	}

	@Override
	public <T> Map<String, BeanReference<T>> namedConfiguredForRole(Class<T> role) {
		throw notSupported();
	}

	private AssertionFailure notSupported() {
		return new AssertionFailure( "This method is not supported in this test implementation" );
	}
}
