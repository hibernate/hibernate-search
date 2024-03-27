/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.spi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
			List<Throwable> failures = new ArrayList<>();
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
				delegate.appendChildrenFailuresTo( failures, builder );
			}
			builder.endObject();
			throw log.collectedFailures( process, builder.toString(), failures );
		}
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

		EventContext createEventContext(EventContextElement contextElement) {
			return EventContext.create( contextElement );
		}

		final void appendChildrenFailuresTo(List<Throwable> failures, ToStringTreeBuilder builder) {
			for ( ContextualFailureCollectorImpl child : children.values() ) {
				// Some contexts may have been mentioned without any failure being ever reported.
				// Only display contexts that had at least one failure reported.
				if ( child.hasFailure() ) {
					child.appendFailuresTo( failures, builder );
				}
			}
		}

		final Collection<ContextualFailureCollectorImpl> children() {
			return children.values();
		}

	}

	private static class ContextualFailureCollectorImpl extends NonRootFailureCollector implements ContextualFailureCollector {
		private final NonRootFailureCollector parent;
		private final EventContextElement contextElement;

		// Avoiding blocking implementations because we access this from reactive event loops
		private final Collection<Throwable> failures = new ConcurrentLinkedDeque<>();
		private final Collection<String> failureMessages = new ConcurrentLinkedDeque<>();

		private ContextualFailureCollectorImpl(NonRootFailureCollector parent, EventContextElement contextElement) {
			super( parent );
			this.parent = parent;
			this.contextElement = contextElement;
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
			doAdd( null, failureMessage );
		}

		@Override
		ContextualFailureCollectorImpl withDefaultContext() {
			return this;
		}

		@Override
		public EventContext eventContext() {
			return parent.createEventContext( contextElement );
		}

		@Override
		EventContext createEventContext(EventContextElement contextElement) {
			return eventContext().append( contextElement );
		}

		void appendFailuresTo(List<Throwable> failures, ToStringTreeBuilder builder) {
			builder.startObject( contextElement.render() );
			failures.addAll( this.failures );
			if ( !failureMessages.isEmpty() ) {
				builder.attribute( EngineEventContextMessages.INSTANCE.failureReportFailures(), failureMessages );
			}
			appendChildrenFailuresTo( failures, builder );
			builder.endObject();
		}

		private void doAdd(Throwable failure, String failureMessage) {
			log.newCollectedFailure( root.process, this, failure );

			if ( root.shouldAddFailure() ) {
				failureMessages.add( failureMessage );
				if ( failure != null ) {
					failures.add( failure );
				}
			}
		}
	}

}
