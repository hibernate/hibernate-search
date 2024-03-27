/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.reflect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.acme.ClassNotInHibernateSearchPackage;

class RuntimeHelperTest {

	@Test
	void callerClassWalker() {
		Optional<Class<?>> caller = RuntimeHelper.callerClassWalker().walk( Stream::findFirst );
		assertThat( caller )
				.contains( RuntimeHelperTest.class );

		List<Class<?>> callers = Delegate.walkInClass( stream -> stream.limit( 2 ).collect( Collectors.toList() ) );
		assertThat( callers )
				.containsExactly( Delegate.class, RuntimeHelperTest.class );
	}

	@Test
	void firstNonJdkCaller() {
		Supplier<Optional<Class<?>>> called = new Supplier<Optional<Class<?>>>() {
			@Override
			public Optional<Class<?>> get() {
				return RuntimeHelper.firstNonSelfNonJdkCaller();
			}
		};
		assertThat( Delegate.call( called ) )
				.contains( Delegate.class );
		assertThat( ClassNotInHibernateSearchPackage.call( called ) )
				.contains( ClassNotInHibernateSearchPackage.class );
	}

	// StackWalker ignores lambda by default, so this is important
	@Test
	void firstNonJdkCaller_lambda() {
		Supplier<Optional<Class<?>>> called = () -> RuntimeHelper.firstNonSelfNonJdkCaller();
		assertThat( Delegate.<Optional<Class<?>>>call( called ) )
				.contains( Delegate.class );
		assertThat( ClassNotInHibernateSearchPackage.<Optional<Class<?>>>call( called ) )
				.contains( ClassNotInHibernateSearchPackage.class );
	}

	@Test
	void isHibernateSearch() {
		assertThat( RuntimeHelper.isHibernateSearch( Delegate.class ) )
				.isTrue();
		assertThat( RuntimeHelper.isHibernateSearch( ClassNotInHibernateSearchPackage.class ) )
				.isFalse();
	}

	private static class Delegate {
		static <T> T walkInClass(Function<? super Stream<Class<?>>, ? extends T> function) {
			return RuntimeHelper.callerClassWalker().walk( function );
		}

		static <T> T call(Supplier<T> supplier) {
			return supplier.get();
		}
	}

}
