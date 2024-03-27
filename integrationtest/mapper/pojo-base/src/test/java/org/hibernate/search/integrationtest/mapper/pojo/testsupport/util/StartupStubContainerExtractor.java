/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.util;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

/**
 * A stub container value extractors for use in tests where the extractor is only used on startup.
 * <p>
 * This is useful if we know the bridge will be filtered out, or simply if we don't test runtime at all.
 * <p>
 * Any runtime use of this extractor will simply increment a counter and throw an exception.
 */
public class StartupStubContainerExtractor implements ContainerExtractor<Object, Object> {
	public static String NAME = "stub-container-extractor";

	public static class CounterKeys {
		public final StaticCounters.Key instance = StaticCounters.createKey();
		public final StaticCounters.Key runtimeUse = StaticCounters.createKey();
		public final StaticCounters.Key holderClose = StaticCounters.createKey();

		private CounterKeys() {
		}
	}

	public static CounterKeys createKeys() {
		return new CounterKeys();
	}

	public static BeanHolder<StartupStubContainerExtractor> create(CounterKeys counterKeys) {
		StartupStubContainerExtractor bridge = new StartupStubContainerExtractor( counterKeys );
		return new CloseCountingBeanHolder<>( bridge, counterKeys.holderClose );
	}

	public final CounterKeys counterKeys;

	private StartupStubContainerExtractor(CounterKeys counterKeys) {
		StaticCounters.get().increment( counterKeys.instance );
		this.counterKeys = counterKeys;
	}

	@Override
	public <T, C2> void extract(Object container, ValueProcessor<T, ? super Object, C2> perValueProcessor, T target,
			C2 context, ContainerExtractionContext extractionContext) {
		throw unexpectedRuntimeUse();
	}

	private AssertionFailure unexpectedRuntimeUse() {
		StaticCounters.get().increment( counterKeys.runtimeUse );
		return new AssertionFailure(
				"Instances of " + getClass().getSimpleName() + " are not supposed to be used at runtime,"
						+ " they should only be used to test the startup process."
		);
	}
}
