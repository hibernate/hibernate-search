/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.reflect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ParameterizedPerClass
class ParameterizedClassExtensionTest {

	// Arguments for a parameterized class setup.
	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( "string1", 1, true ),
				Arguments.of( "string2", 2, true ),
				Arguments.of( "string3", 3, true )
		);
	}

	// Arguments for a parameterized test.
	public static List<? extends Arguments> testParams() {
		return Arrays.asList(
				Arguments.of( "text1" ),
				Arguments.of( "text2" ),
				Arguments.of( "text3" )
		);
	}

	private Context context;
	private static int test1ExecutionCount = 0;
	private static int test2ExecutionCount = 0;


	// Setup method that will use the params method to configure the test execution
	// and will run all the @Test/@ParameterizedTest tests within this class.
	@ParameterizedSetup
	@MethodSource("params")
	void env(String string, int number, boolean bool) {
		// configure test class for a set of config arguments
		context = new Context( string, number, bool );
		System.err.println( "init" );
	}

	// A simple test.
	// Will be executed once after each class setup execution.
	@Test
	void test1() {
		assertThat( context ).isNotNull()
				.isEqualTo( new Context( params().get( test1ExecutionCount ) ) );
		test1ExecutionCount++;
	}

	// A parameterized test within a parameterized class.
	// Will be executed for each set of arguments provided by the test parameter source after each class setup execution.
	@ParameterizedTest
	@MethodSource("testParams")
	void test2(String string) {
		assertThat( context ).isNotNull()
				.isEqualTo( new Context( params().get( test2ExecutionCount / 3 ) ) );
		test2ExecutionCount++;
		assertThat( string ).startsWith( "text" );
	}

	private static class Context {
		private final String string;
		private final int number;
		private final boolean bool;

		private Context(String string, int number, boolean bool) {
			this.string = string;
			this.number = number;
			this.bool = bool;
		}

		public Context(Arguments arguments) {
			this( (String) arguments.get()[0], (Integer) arguments.get()[1], (Boolean) arguments.get()[2] );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Context context = (Context) o;
			return number == context.number && bool == context.bool && Objects.equals( string, context.string );
		}

		@Override
		public int hashCode() {
			return Objects.hash( string, number, bool );
		}

		@Override
		public String toString() {
			return "Context{" +
					"string='" + string + '\'' +
					", number=" + number +
					", bool=" + bool +
					'}';
		}
	}
}
