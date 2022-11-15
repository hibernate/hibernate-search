/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import org.hibernate.search.util.impl.test.function.ThrowingBiConsumer;
import org.hibernate.search.util.impl.test.function.ThrowingConsumer;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class ComposedExtension
		implements AfterAllCallback, AfterEachCallback, AfterTestExecutionCallback,
		BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback, TestExecutionExceptionHandler {

	private static final ThrowingConsumer<ExtensionContext, Exception> DO_NOTHING = extensionContext -> {};
	private static final ThrowingBiConsumer<ExtensionContext, Throwable, Throwable> BI_DO_NOTHING = (extensionContext, e) -> {};

	private final FullExtension inner;
	private final FullExtension outer;

	public ComposedExtension(Extension inner, Extension outer) {
		this.inner = new FullExtension( inner );
		this.outer = new FullExtension( outer );
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		inner.afterAll( context );
		outer.afterAll( context );
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		inner.afterEach( context );
		outer.afterEach( context );
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		inner.afterTestExecution( context );
		outer.afterTestExecution( context );
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		outer.beforeAll( context );
		inner.beforeAll( context );
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		outer.beforeEach( context );
		inner.beforeEach( context );
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		outer.beforeTestExecution( context );
		inner.beforeTestExecution( context );
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		Throwable thr = throwable;
		try {
			inner.handleTestExecutionException( context, thr );
		}
		catch (Throwable e) {
			if ( e != throwable ) {
				e.addSuppressed( throwable );
			}
			thr = e;
			throw thr;
		}
		finally {
			outer.handleTestExecutionException( context, thr );
		}
	}

	public static class FullExtension
			implements AfterAllCallback, AfterEachCallback, AfterTestExecutionCallback,
			BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback, TestExecutionExceptionHandler {

		private final ThrowingConsumer<ExtensionContext, Exception> afterAllCallback;
		private final ThrowingConsumer<ExtensionContext, Exception> afterEachCallback;
		private final ThrowingConsumer<ExtensionContext, Exception> afterTestExecutionCallback;
		private final ThrowingConsumer<ExtensionContext, Exception> beforeAllCallback;
		private final ThrowingConsumer<ExtensionContext, Exception> beforeEachCallback;
		private final ThrowingConsumer<ExtensionContext, Exception> beforeTestExecutionCallback;
		private final ThrowingBiConsumer<ExtensionContext, Throwable, Throwable> testExecutionExceptionHandler;

		public FullExtension(Extension extension) {
			this.afterAllCallback =
					extension instanceof AfterAllCallback ? ( (AfterAllCallback) extension )::afterAll : DO_NOTHING;
			this.afterEachCallback =
					extension instanceof AfterEachCallback ? ( (AfterEachCallback) extension )::afterEach : DO_NOTHING;
			this.afterTestExecutionCallback = extension instanceof AfterTestExecutionCallback
					? ( (AfterTestExecutionCallback) extension )::afterTestExecution
					: DO_NOTHING;
			this.beforeAllCallback =
					extension instanceof BeforeAllCallback ? ( (BeforeAllCallback) extension )::beforeAll : DO_NOTHING;
			this.beforeEachCallback =
					extension instanceof BeforeEachCallback ? ( (BeforeEachCallback) extension )::beforeEach : DO_NOTHING;
			this.beforeTestExecutionCallback = extension instanceof BeforeTestExecutionCallback
					? ( (BeforeTestExecutionCallback) extension )::beforeTestExecution
					: DO_NOTHING;
			this.testExecutionExceptionHandler = extension instanceof TestExecutionExceptionHandler
					? ( (TestExecutionExceptionHandler) extension )::handleTestExecutionException
					: BI_DO_NOTHING;
		}

		public FullExtension(ThrowingConsumer<ExtensionContext, Exception> afterAllCallback,
				ThrowingConsumer<ExtensionContext, Exception> afterEachCallback,
				ThrowingConsumer<ExtensionContext, Exception> afterTestExecutionCallback,
				ThrowingConsumer<ExtensionContext, Exception> beforeAllCallback,
				ThrowingConsumer<ExtensionContext, Exception> beforeEachCallback,
				ThrowingConsumer<ExtensionContext, Exception> beforeTestExecutionCallback,
				ThrowingBiConsumer<ExtensionContext, Throwable, Throwable> testExecutionExceptionHandler) {
			this.afterAllCallback = afterAllCallback;
			this.afterEachCallback = afterEachCallback;
			this.afterTestExecutionCallback = afterTestExecutionCallback;
			this.beforeAllCallback = beforeAllCallback;
			this.beforeEachCallback = beforeEachCallback;
			this.beforeTestExecutionCallback = beforeTestExecutionCallback;
			this.testExecutionExceptionHandler = testExecutionExceptionHandler;
		}

		@Override
		public void afterAll(ExtensionContext context) throws Exception {
			afterAllCallback.accept( context );
		}

		@Override
		public void afterEach(ExtensionContext context) throws Exception {
			afterEachCallback.accept( context );
		}

		@Override
		public void afterTestExecution(ExtensionContext context) throws Exception {
			afterTestExecutionCallback.accept( context );
		}

		@Override
		public void beforeAll(ExtensionContext context) throws Exception {
			beforeAllCallback.accept( context );
		}

		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			beforeEachCallback.accept( context );
		}

		@Override
		public void beforeTestExecution(ExtensionContext context) throws Exception {
			beforeTestExecutionCallback.accept( context );
		}

		@Override
		public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
			testExecutionExceptionHandler.accept( context, throwable );
		}

		public static class Builder {

			private static final ThrowingConsumer<ExtensionContext, Exception> DO_NOTHING = c -> {};
			private static final ThrowingBiConsumer<ExtensionContext, Throwable, Throwable> JUST_RETHROW =
					(c, e) -> { throw e; };

			private ThrowingConsumer<ExtensionContext, Exception> afterAllCallback = DO_NOTHING;
			private ThrowingConsumer<ExtensionContext, Exception> afterEachCallback = DO_NOTHING;
			private ThrowingConsumer<ExtensionContext, Exception> afterTestExecutionCallback = DO_NOTHING;
			private ThrowingConsumer<ExtensionContext, Exception> beforeAllCallback = DO_NOTHING;
			private ThrowingConsumer<ExtensionContext, Exception> beforeEachCallback = DO_NOTHING;
			private ThrowingConsumer<ExtensionContext, Exception> beforeTestExecutionCallback = DO_NOTHING;
			private ThrowingBiConsumer<ExtensionContext, Throwable, Throwable> testExecutionExceptionHandler = JUST_RETHROW;

			public Builder withAfterAll(ThrowingConsumer<ExtensionContext, Exception> afterAllCallback) {
				this.afterAllCallback = afterAllCallback;
				return this;
			}

			public Builder withAfterEach(ThrowingConsumer<ExtensionContext, Exception> afterEachCallback) {
				this.afterEachCallback = afterEachCallback;
				return this;
			}

			public Builder withAfterTestExecution(
					ThrowingConsumer<ExtensionContext, Exception> afterTestExecutionCallback) {
				this.afterTestExecutionCallback = afterTestExecutionCallback;
				return this;
			}

			public Builder withBeforeAll(ThrowingConsumer<ExtensionContext, Exception> beforeAllCallback) {
				this.beforeAllCallback = beforeAllCallback;
				return this;
			}

			public Builder withBeforeEach(ThrowingConsumer<ExtensionContext, Exception> beforeEachCallback) {
				this.beforeEachCallback = beforeEachCallback;
				return this;
			}

			public Builder withBeforeTestExecution(
					ThrowingConsumer<ExtensionContext, Exception> beforeTestExecutionCallback) {
				this.beforeTestExecutionCallback = beforeTestExecutionCallback;
				return this;
			}

			public Builder withTestExecutionExceptionHandler(
					ThrowingBiConsumer<ExtensionContext, Throwable, Throwable> testExecutionExceptionHandler) {
				this.testExecutionExceptionHandler = testExecutionExceptionHandler;
				return this;
			}

			public FullExtension build() {
				return new FullExtension(
						afterAllCallback,
						afterEachCallback,
						afterTestExecutionCallback,
						beforeAllCallback,
						beforeEachCallback,
						beforeTestExecutionCallback,
						testExecutionExceptionHandler
				);
			}
		}
	}

}
