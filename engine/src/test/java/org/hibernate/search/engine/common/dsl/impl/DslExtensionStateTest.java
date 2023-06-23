/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.dsl.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.util.common.SearchException;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class DslExtensionStateTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private Function<Object, MyResultType> contextFunction;

	private final DslExtensionState<MyResultType> state = new DslExtensionState<>();

	private final MyResultType expectedResult = new MyResultType();

	@Test
	public void ifSupported_noSupported() {
		String extensionToString = "EXTENSION_TO_STRING";
		state.ifSupported( new MyExtension( extensionToString ), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void ifSupported_supported() {
		Object extendedContext = new Object();

		when( contextFunction.apply( extendedContext ) ).thenReturn( expectedResult );
		state.ifSupported( new MyExtension(), Optional.of( extendedContext ), contextFunction );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void orElse() {
		Object defaultContext = new Object();

		when( contextFunction.apply( defaultContext ) ).thenReturn( expectedResult );
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void orElseFail() {
		assertThatThrownBy( state::orElseFail )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"None of the provided extensions can be applied to the current context",
						Collections.emptyList().toString()
				);
	}

	@Test
	public void ifSupportedThenOrElseFail_noSupported() {
		String extensionToString = "EXTENSION_TO_STRING";
		state.ifSupported( new MyExtension( extensionToString ), Optional.empty(), contextFunction );
		assertThatThrownBy( state::orElseFail )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"None of the provided extensions can be applied to the current context",
						extensionToString
				);
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void ifSupportedThenOrElseFail_supported() {
		Object extendedContext = new Object();

		when( contextFunction.apply( extendedContext ) ).thenReturn( expectedResult );
		state.ifSupported( new MyExtension(), Optional.of( extendedContext ), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.orElseFail();
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void ifSupportedThenOrElse_noSupported() {
		Object defaultContext = new Object();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		when( contextFunction.apply( defaultContext ) ).thenReturn( expectedResult );
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void ifSupportedThenOrElse_supported() {
		Object extendedContext = new Object();
		Object defaultContext = new Object();

		when( contextFunction.apply( extendedContext ) ).thenReturn( expectedResult );
		state.ifSupported( new MyExtension(), Optional.of( extendedContext ), contextFunction );
		verifyNoOtherInteractionsAndReset();

		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void multipleIfSupported_noSupported() {
		String extension1ToString = "EXTENSION1_TO_STRING";
		String extension2ToString = "EXTENSION2_TO_STRING";
		String extension3ToString = "EXTENSION3_TO_STRING";

		state.ifSupported( new MyExtension( extension1ToString ), Optional.empty(), contextFunction );
		state.ifSupported( new MyExtension( extension2ToString ), Optional.empty(), contextFunction );
		state.ifSupported( new MyExtension( extension3ToString ), Optional.empty(), contextFunction );
		assertThatThrownBy( state::orElseFail )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"None of the provided extensions can be applied to the current context",
						extension1ToString,
						extension2ToString,
						extension3ToString
				);
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void multipleIfSupported_singleSupported() {
		Object extendedContext1 = new Object();

		when( contextFunction.apply( extendedContext1 ) ).thenReturn( expectedResult );
		state.ifSupported( new MyExtension(), Optional.of( extendedContext1 ), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.orElseFail();
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void multipleIfSupported_multipleSupported() {
		Object extendedContext2 = new Object();
		Object extendedContext3 = new Object();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		when( contextFunction.apply( extendedContext2 ) ).thenReturn( expectedResult );
		state.ifSupported( new MyExtension(), Optional.of( extendedContext2 ), contextFunction );
		verifyNoOtherInteractionsAndReset();

		// Only the first supported extension should be applied
		state.ifSupported( new MyExtension(), Optional.of( extendedContext3 ), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.orElseFail();
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void multipleIfSupportedThenOrElse_noSupported() {
		Object defaultContext = new Object();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		when( contextFunction.apply( defaultContext ) ).thenReturn( expectedResult );
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void multipleIfSupportedThenOrElse_singleSupported() {
		Object extendedContext1 = new Object();
		Object defaultContext = new Object();

		when( contextFunction.apply( extendedContext1 ) ).thenReturn( expectedResult );
		state.ifSupported( new MyExtension(), Optional.of( extendedContext1 ), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void multipleIfSupportedThenOrElse_multipleSupported() {
		Object extendedContext2 = new Object();
		Object extendedContext3 = new Object();
		Object defaultContext = new Object();

		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyNoOtherInteractionsAndReset();

		when( contextFunction.apply( extendedContext2 ) ).thenReturn( expectedResult );
		state.ifSupported( new MyExtension(), Optional.of( extendedContext2 ), contextFunction );
		verifyNoOtherInteractionsAndReset();

		// Only the first supported extension should be applied
		state.ifSupported( new MyExtension(), Optional.of( extendedContext3 ), contextFunction );
		verifyNoOtherInteractionsAndReset();

		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void orElseThenIfSupported() {
		Object defaultContext = new Object();

		when( contextFunction.apply( defaultContext ) ).thenReturn( expectedResult );
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyNoOtherInteractionsAndReset();

		assertThatThrownBy( () -> state.ifSupported( new MyExtension(), Optional.empty(), contextFunction ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid call of ifSupported(...) after orElse(...)"
				);
		verifyNoOtherInteractionsAndReset();
	}

	@SuppressWarnings("unchecked")
	private void verifyNoOtherInteractionsAndReset() {
		verifyNoMoreInteractions( contextFunction );
		reset( contextFunction );
	}

	private static class MyExtension {
		private final String name;

		MyExtension() {
			this( null );
		}

		MyExtension(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			if ( name != null ) {
				return name;
			}
			else {
				return super.toString();
			}
		}
	}

	private static class MyResultType {
	}

}
