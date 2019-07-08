/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.EventContextElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.common.reporting.impl.CommonEventContextMessages;

import org.jboss.logging.Messages;

public class RootFailureCollector implements FailureCollector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final CommonEventContextMessages COMMON_MESSAGES = Messages.getBundle( CommonEventContextMessages.class );

	private static final EngineEventContextMessages ENGINE_MESSAGES = Messages.getBundle( EngineEventContextMessages.class );

	/**
	 * This prevents Hibernate Search from trying too hard to collect errors,
	 * which could be a problem when there is something fundamentally wrong
	 * that will cause almost every operation to fail.
	 */
	private final int failureLimit;

	private NonRootFailureCollector delegate;
	private int failureCount = 0;

	public RootFailureCollector(int failureLimit) {
		this.failureLimit = failureLimit;
	}

	public void checkNoFailure() {
		if ( failureCount > 0 ) {
			String renderedFailures = renderFailures();
			throw log.bootstrapCollectedFailures( renderedFailures );
		}
	}

	private String renderFailures() {
		ToStringStyle style = ToStringStyle.multilineIndentStructure(
				ENGINE_MESSAGES.failureReportContextFailuresSeparator(),
				ENGINE_MESSAGES.failureReportContextIndent(),
				ENGINE_MESSAGES.failureReportFailuresBulletPoint(),
				ENGINE_MESSAGES.failureReportFailuresNoBulletPoint()
		);
		ToStringTreeBuilder builder = new ToStringTreeBuilder( style );
		if ( delegate != null ) {
			delegate.appendFailuresTo( builder );
		}
		return builder.toString();
	}

	@Override
	public ContextualFailureCollector withContext(EventContext context) {
		if ( delegate == null ) {
			delegate = new NonRootFailureCollector( this );
		}
		return delegate.withContext( context );
	}

	@Override
	public ContextualFailureCollector withContext(EventContextElement contextElement) {
		if ( delegate == null ) {
			delegate = new NonRootFailureCollector( this );
		}
		return delegate.withContext( contextElement );
	}

	private void onAddFailure() {
		++failureCount;
		if ( failureCount >= failureLimit ) {
			String renderedFailures = renderFailures();
			throw log.boostrapCollectedFailureLimitReached( renderedFailures, failureCount );
		}
	}

	private static class NonRootFailureCollector implements FailureCollector {
		protected final RootFailureCollector root;
		private Map<EventContextElement, ContextualFailureCollectorImpl> children;

		private NonRootFailureCollector(RootFailureCollector root) {
			this.root = root;
		}

		protected NonRootFailureCollector(NonRootFailureCollector parent) {
			this.root = parent.root;
		}

		@Override
		public ContextualFailureCollectorImpl withContext(EventContext context) {
			List<EventContextElement> elements = context.getElements();
			// This should not happen, but we want to be extra-cautious to avoid failures while handling failures
			if ( elements.isEmpty() ) {
				// Just log the problem and degrade gracefully.
				log.unexpectedEmptyEventContext( new IllegalStateException( "Exception for stack trace" ) );
				return withDefaultContext();
			}
			else {
				NonRootFailureCollector failureCollector = this;
				for ( EventContextElement contextElement : elements ) {
					failureCollector = failureCollector.withContext( contextElement );
				}
				return (ContextualFailureCollectorImpl) failureCollector;
			}
		}

		@Override
		public ContextualFailureCollectorImpl withContext(EventContextElement contextElement) {
			if ( children == null ) {
				// Use a LinkedHashMap for deterministic iteration
				children = new LinkedHashMap<>();
			}
			ContextualFailureCollectorImpl child = children.get( contextElement );
			if ( child != null ) {
				return child;
			}
			else {
				child = new ContextualFailureCollectorImpl( this, contextElement );
				children.put( contextElement, child );
				return child;
			}
		}

		ContextualFailureCollectorImpl withDefaultContext() {
			return withContext( EventContexts.getDefault() );
		}

		void appendContextTo(StringJoiner joiner) {
			// Nothing to do
		}

		void appendFailuresTo(ToStringTreeBuilder builder) {
			builder.startObject();
			appendChildrenFailuresTo( builder );
			builder.endObject();
		}

		final void appendChildrenFailuresTo(ToStringTreeBuilder builder) {
			if ( children != null ) {
				for ( ContextualFailureCollectorImpl child : children.values() ) {
					// Some contexts may have been mentioned without any failure being ever reported.
					// Only display contexts that had at least one failure reported.
					if ( child.hasFailure() ) {
						child.appendFailuresTo( builder );
					}
				}
			}
		}

		final Map<EventContextElement, ContextualFailureCollectorImpl> getChildren() {
			return children != null ? children : Collections.emptyMap();
		}
	}

	private static class ContextualFailureCollectorImpl extends NonRootFailureCollector implements ContextualFailureCollector {
		private final NonRootFailureCollector parent;
		private final EventContextElement context;

		private List<String> failureMessages;

		private ContextualFailureCollectorImpl(NonRootFailureCollector parent, EventContextElement context) {
			super( parent );
			this.parent = parent;
			this.context = context;
		}

		@Override
		public boolean hasFailure() {
			if ( failureMessages != null && !failureMessages.isEmpty() ) {
				return true;
			}
			for ( ContextualFailureCollectorImpl child : getChildren().values() ) {
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
				EventContext eventContext = e.getContext();
				if ( eventContext != null ) {
					failureCollector = failureCollector.withContext( e.getContext() );
				}
				// Do not include the context in the failure message, since we will render it as part of the failure report
				failureCollector.doAdd( e, e.getMessageWithoutContext() );
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

		@Override
		void appendFailuresTo(ToStringTreeBuilder builder) {
			builder.startObject( context.render() );
			if ( failureMessages != null ) {
				builder.startList( ENGINE_MESSAGES.failureReportFailures() );
				for ( String failureMessage : failureMessages ) {
					builder.value( failureMessage );
				}
				builder.endList();
			}
			appendChildrenFailuresTo( builder );
			builder.endObject();
		}

		private void doAdd(Throwable failure, String failureMessage) {
			StringJoiner contextJoiner = new StringJoiner( COMMON_MESSAGES.contextSeparator() );
			appendContextTo( contextJoiner );
			log.newBootstrapCollectedFailure( contextJoiner.toString(), failure );

			doAdd( failureMessage );
		}

		private void doAdd(String failureMessage) {
			if ( failureMessages == null ) {
				failureMessages = new ArrayList<>();
			}
			failureMessages.add( failureMessage );

			root.onAddFailure();
		}
	}

}
