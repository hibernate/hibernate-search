/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.impl.LogFailureHandler;

public class EngineBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				ThreadProvider.class, EmbeddedThreadProvider.NAME,
				beanResolver -> BeanHolder.of( new EmbeddedThreadProvider() )
		);
		context.define(
				FailureHandler.class, LogFailureHandler.NAME,
				beanResolver -> BeanHolder.of( new LogFailureHandler() )
		);
	}
}
