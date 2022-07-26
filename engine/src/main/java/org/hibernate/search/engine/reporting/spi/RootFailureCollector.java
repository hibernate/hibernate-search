/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.impl.EngineEventContextMessages;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.data.impl.InsertionOrder;
import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.EventContextElement;
import org.hibernate.search.util.common.reporting.impl.CommonEventContextMessages;

public final class RootFailureCollector implements FailureCollector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	/**
	 * This prevents Hibernate Search from trying too hard to collect errors,
	 * which could be a problem when there is something fundamentally wrong
	 * that will cause almost every operation to fail.
	 */
	// Exposed for tests
	static final int FAILURE_LIMIT = 100;

	private final String process;
	private final NonRootFailureCollector delegate;
	private final AtomicInteger failureCount = new AtomicInteger();

	public RootFailureCollector(String process) {
		this.process = process;
		this.delegate = new NonRootFailureCollector( this );
	}

	public void checkNoFailure() {
		if ( failureCount.get() > 0 ) {
			String renderedFailures = renderFailures();
			throw log.collectedFailures( process, renderedFailures );
		}
	}

	private String renderFailures() {
		ToStringStyle style = ToStringStyle.multilineIndentStructure(
				EngineEventContextMessages.INSTANCE.failureReportContextFailuresSeparator(),
				EngineEventContextMessages.INSTANCE.failureReportContextIndent(),
				EngineEventContextMessages.INSTANCE.failureReportFailuresBulletPoint(),
				EngineEventContextMessages.INSTANCE.failureReportFailuresNoBulletPoint()
		);
		ToStringTreeBuilder builder = new ToStringTreeBuilder( style );
		builder.startObject();
		if ( failureCount.get() > FAILURE_LIMIT ) {
			builder.value( log.collectedFailureLimitReached( process, FAILURE_LIMIT, failureCount.get() ) );
		}
		if ( delegate != null ) {
			delegate.appendChildrenFailuresTo( builder );
		}
		builder.endObject();
		return builder.toString();
	}

	@Override
	public ContextualFailureCollector withContext(EventContext context) {
		return delegate.withContext( context );
	}

	@Override
	public ContextualFailureCollector withContext(EventContextElement contextElement) {
		return delegate.withContext( contextElement );
	}

	private boolean shouldAddFailure() {
		return failureCount.incrementAndGet() <= FAILURE_LIMIT;
	}

	private static class NonRootFailureCollector implements FailureCollector {
		protected final RootFailureCollector root;
		private final InsertionOrder<EventContextElement> childrenInsertionOrder = new InsertionOrder<>();
		// Avoiding blocking implementations because we access this from reactive event loops
		private final Map<InsertionOrder.Key<EventContextElement>, ContextualFailureCollectorImpl> children =
				new ConcurrentSkipListMap<>();

		private NonRootFailureCollector(RootFailureCollector root) {
			this.root = root;
		}

		protected NonRootFailureCollector(NonRootFailureCollector parent) {
			this.root = parent.root;
		}

		@Override
		public ContextualFailureCollectorImpl withContext(EventContext context) {
			if ( context == null ) {
				return withDefaultContext();
			}
			List<EventContextElement> elements = context.elements();
			try {
				NonRootFailureCollector failureCollector = this;
				for ( EventContextElement contextElement : elements ) {
					failureCollector = failureCollector.withContext( contextElement );
				}
				return (ContextualFailureCollectorImpl) failureCollector;
			}
			// This should not happen, but we want to be extra-cautious to avoid failures while handling failures
			catch (RuntimeException e) {
				// Just log the problem and degrade gracefully.
				log.exceptionWhileCollectingFailure( e.getMessage(), e );
				return withDefaultContext();
			}
		}

		@Override
		public ContextualFailureCollectorImpl withContext(EventContextElement contextElement) {
			if ( contextElement == null ) {
				return withDefaultContext();
			}
			return children.computeIfAbsent(
					childrenInsertionOrder.wrapKey( contextElement ),
					key -> new ContextualFailureCollectorImpl( this, key.get() )
			);
		}

		ContextualFailureCollectorImpl withDefaultContext() {
			return withContext( EventContexts.defaultContext() );
		}

		void appendContextTo(StringJoiner joiner) {
			// Nothing to do
		}

		final void appendChildrenFailuresTo(ToStringTreeBuilder builder) {
			for ( ContextualFailureCollectorImpl child : children.values() ) {
				// Some contexts may have been mentioned without any failure being ever reported.
				// Only display contexts that had at least one failure reported.
				if ( child.hasFailure() ) {
					child.appendFailuresTo( builder );
				}
			}
		}

		final Collection<ContextualFailureCollectorImpl> children() {
			return children.values();
		}
	}

	private static class ContextualFailureCollectorImpl extends NonRootFailureCollector implements ContextualFailureCollector {
		private final NonRootFailureCollector parent;
		private final EventContextElement context;

		// Avoiding blocking implementations because we access this from reactive event loops
		private final Collection<String> failureMessages = new ConcurrentLinkedDeque<>();

		private ContextualFailureCollectorImpl(NonRootFailureCollector parent, EventContextElement context) {
			super( parent );
			this.parent = parent;
			this.context = context;
		}

		@Override
		public boolean hasFailure() {
			if ( !failureMessages.isEmpty() ) {
				return true;
			}
			for ( ContextualFailureCollectorImpl child : children() ) {
				if ( child.hasFailure() ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void add(Throwable t) {
			if ( t instanceof SearchException ) {
				SearchException e = (SearchException) t;
				ContextualFailureCollectorImpl failureCollector = this;
				EventContext eventContext = e.context();
				if ( eventContext != null ) {
					failureCollector = failureCollector.withContext( e.context() );
				}
				// Do not include the context in the failure message, since we will render it as part of the failure report
				failureCollector.doAdd( e, e.messageWithoutContext() );
			}
			else {
				doAdd( t, t.getMessage() );
			}
		}

		@Override
		public void add(String failureMessage) {
			doAdd( failureMessage );
		}

		@Override
		ContextualFailureCollectorImpl withDefaultContext() {
			return this;
		}

		@Override
		void appendContextTo(StringJoiner joiner) {
			parent.appendContextTo( joiner );
			joiner.add( context.render() );
		}

		void appendFailuresTo(ToStringTreeBuilder builder) {
			builder.startObject( context.render() );
			if ( !failureMessages.isEmpty() ) {
				builder.attribute( EngineEventContextMessages.INSTANCE.failureReportFailures(), failureMessages );
			}
			appendChildrenFailuresTo( builder );
			builder.endObject();
		}

		private void doAdd(Throwable failure, String failureMessage) {
			StringJoiner contextJoiner = new StringJoiner( CommonEventContextMessages.INSTANCE.contextSeparator() );
			appendContextTo( contextJoiner );
			log.newCollectedFailure( root.process, contextJoiner.toString(), failure );

			doAdd( failureMessage );
		}

		private void doAdd(String failureMessage) {
			if ( root.shouldAddFailure() ) {
				failureMessages.add( failureMessage );
			}
		}
	}

}
