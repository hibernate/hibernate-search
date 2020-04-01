/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.dsl.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class DslExtensionStateTest extends EasyMockSupport {

	private final Function<Object, MyResultType> contextFunction = createMock( Function.class );

	private final DslExtensionState<MyResultType> state = new DslExtensionState<>();

	private final MyResultType expectedResult = new MyResultType();

	@Test
	public void ifSupported_noSupported() {
		String extensionToString = "EXTENSION_TO_STRING";
		resetAll();
		replayAll();
		state.ifSupported( new MyExtension( extensionToString ), Optional.empty(), contextFunction );
		verifyAll();
	}

	@Test
	public void ifSupported_supported() {
		Object extendedContext = new Object();

		resetAll();
		EasyMock.expect( contextFunction.apply( extendedContext ) ).andReturn( expectedResult );
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext ), contextFunction );
		verifyAll();
	}

	@Test
	public void orElse() {
		Object defaultContext = new Object();

		resetAll();
		EasyMock.expect( contextFunction.apply( defaultContext ) ).andReturn( expectedResult );
		replayAll();
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyAll();
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
		resetAll();
		replayAll();
		state.ifSupported( new MyExtension( extensionToString ), Optional.empty(), contextFunction );
		assertThatThrownBy( state::orElseFail )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"None of the provided extensions can be applied to the current context",
						extensionToString
				);
		verifyAll();
	}

	@Test
	public void ifSupportedThenOrElseFail_supported() {
		Object extendedContext = new Object();

		resetAll();
		EasyMock.expect( contextFunction.apply( extendedContext ) ).andReturn( expectedResult );
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext ), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.orElseFail();
		verifyAll();
	}

	@Test
	public void ifSupportedThenOrElse_noSupported() {
		Object defaultContext = new Object();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		EasyMock.expect( contextFunction.apply( defaultContext ) ).andReturn( expectedResult );
		replayAll();
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyAll();
	}

	@Test
	public void ifSupportedThenOrElse_supported() {
		Object extendedContext = new Object();
		Object defaultContext = new Object();

		resetAll();
		EasyMock.expect( contextFunction.apply( extendedContext ) ).andReturn( expectedResult );
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext ), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyAll();
	}

	@Test
	public void multipleIfSupported_noSupported() {
		String extension1ToString = "EXTENSION1_TO_STRING";
		String extension2ToString = "EXTENSION2_TO_STRING";
		String extension3ToString = "EXTENSION3_TO_STRING";

		resetAll();
		replayAll();
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
		verifyAll();
	}

	@Test
	public void multipleIfSupported_singleSupported() {
		Object extendedContext1 = new Object();

		resetAll();
		EasyMock.expect( contextFunction.apply( extendedContext1 ) ).andReturn( expectedResult );
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext1 ), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.orElseFail();
		verifyAll();
	}

	@Test
	public void multipleIfSupported_multipleSupported() {
		Object extendedContext2 = new Object();
		Object extendedContext3 = new Object();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		EasyMock.expect( contextFunction.apply( extendedContext2 ) ).andReturn( expectedResult );
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext2 ), contextFunction );
		verifyAll();

		// Only the first supported extension should be applied
		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext3 ), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.orElseFail();
		verifyAll();
	}

	@Test
	public void multipleIfSupportedThenOrElse_noSupported() {
		Object defaultContext = new Object();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		EasyMock.expect( contextFunction.apply( defaultContext ) ).andReturn( expectedResult );
		replayAll();
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyAll();
	}

	@Test
	public void multipleIfSupportedThenOrElse_singleSupported() {
		Object extendedContext1 = new Object();
		Object defaultContext = new Object();

		resetAll();
		EasyMock.expect( contextFunction.apply( extendedContext1 ) ).andReturn( expectedResult );
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext1 ), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyAll();
	}

	@Test
	public void multipleIfSupportedThenOrElse_multipleSupported() {
		Object extendedContext2 = new Object();
		Object extendedContext3 = new Object();
		Object defaultContext = new Object();

		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.empty(), contextFunction );
		verifyAll();

		resetAll();
		EasyMock.expect( contextFunction.apply( extendedContext2 ) ).andReturn( expectedResult );
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext2 ), contextFunction );
		verifyAll();

		// Only the first supported extension should be applied
		resetAll();
		replayAll();
		state.ifSupported( new MyExtension(), Optional.of( extendedContext3 ), contextFunction );
		verifyAll();

		resetAll();
		replayAll();
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyAll();
	}

	@Test
	public void orElseThenIfSupported() {
		Object defaultContext = new Object();

		resetAll();
		EasyMock.expect( contextFunction.apply( defaultContext ) ).andReturn( expectedResult );
		replayAll();
		assertThat( state.orElse( defaultContext, contextFunction ) ).isSameAs( expectedResult );
		verifyAll();

		resetAll();
		replayAll();
		assertThatThrownBy( () -> state.ifSupported( new MyExtension(), Optional.empty(), contextFunction ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot call ifSupported(...) after orElse(...)"
				);
		verifyAll();
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