/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.hibernate.search.engine.logging.spi.ContextualFailureCollector;
import org.hibernate.search.engine.logging.spi.FailureCollector;
import org.hibernate.search.engine.logging.spi.FailureContextElement;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.ToStringStyle;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

import org.jboss.logging.Messages;

public class RootFailureCollector implements FailureCollector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final FailureContextMessages MESSAGES = Messages.getBundle( FailureContextMessages.class );

	/**
	 * This prevents Hibernate Search from trying too hard to collect errors,
	 * which could be a problem when there is something fundamentally wrong
	 * that will cause almost every operation to fail.
	 */
	private final int failureLimit;

	private FailureCollectorImpl delegate;
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
				MESSAGES.contextFailuresSeparator(),
				MESSAGES.contextIndent(),
				MESSAGES.contextFailuresBulletPoint(),
				MESSAGES.contextFailuresNoBulletPoint()
		);
		ToStringTreeBuilder builder = new ToStringTreeBuilder( style );
		if ( delegate != null ) {
			delegate.appendFailuresTo( builder );
		}
		return builder.toString();
	}

	@Override
	public ContextualFailureCollector withContext(FailureContextElement contextElement) {
		if ( delegate == null ) {
			delegate = new FailureCollectorImpl( this );
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

	private static class FailureCollectorImpl implements FailureCollector {
		protected final RootFailureCollector root;
		private Map<FailureContextElement, ContextualFailureCollectorImpl> children;

		private FailureCollectorImpl(RootFailureCollector root) {
			this.root = root;
		}

		protected FailureCollectorImpl(FailureCollectorImpl parent) {
			this.root = parent.root;
		}

		@Override
		public ContextualFailureCollectorImpl withContext(FailureContextElement contextElement) {
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
					child.appendFailuresTo( builder );
				}
			}
		}
	}

	private static class ContextualFailureCollectorImpl extends FailureCollectorImpl implements ContextualFailureCollector {
		private final FailureCollectorImpl parent;
		private final FailureContextElement context;

		private List<Throwable> failures;

		private ContextualFailureCollectorImpl(FailureCollectorImpl parent, FailureContextElement context) {
			super( parent );
			this.parent = parent;
			this.context = context;
		}

		@Override
		public void add(Throwable t) {
			if ( failures == null ) {
				failures = new ArrayList<>();
			}
			failures.add( t );

			StringJoiner contextJoiner = new StringJoiner( MESSAGES.contextSeparator() );
			appendContextTo( contextJoiner );
			log.newBootstrapCollectedFailure( contextJoiner.toString(), t );

			root.onAddFailure();
		}

		@Override
		void appendContextTo(StringJoiner joiner) {
			parent.appendContextTo( joiner );
			joiner.add( context.render() );
		}

		@Override
		void appendFailuresTo(ToStringTreeBuilder builder) {
			builder.startObject( context.render() );
			if ( failures != null ) {
				builder.startList( MESSAGES.failures() );
				for ( Throwable failure : failures ) {
					builder.value( failure.getMessage() );
				}
				builder.endList();
			}
			appendChildrenFailuresTo( builder );
			builder.endObject();
		}
	}

}
