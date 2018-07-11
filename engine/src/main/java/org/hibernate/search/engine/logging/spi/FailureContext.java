/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import org.hibernate.search.engine.logging.impl.FailureContextMessages;
import org.hibernate.search.util.impl.common.CollectionHelper;

import org.jboss.logging.Messages;

/**
 * A sequence of {@link FailureContextElement}.
 */
public final class FailureContext {

	private static final FailureContextMessages MESSAGES = Messages.getBundle( FailureContextMessages.class );

	public static FailureContext create(FailureContextElement singleElement) {
		return new FailureContext( Collections.singletonList( singleElement ) );
	}

	public static FailureContext create(FailureContextElement firstElement, FailureContextElement ... otherElements) {
		return new FailureContext( Collections.unmodifiableList(
				CollectionHelper.asList( firstElement, otherElements )
		) );
	}

	public static FailureContext create(List<FailureContextElement> elements) {
		return new FailureContext( Collections.unmodifiableList( new ArrayList<>( elements ) ) );
	}

	private final List<FailureContextElement> elements;

	private FailureContext(List<FailureContextElement> elements) {
		this.elements = elements;
	}

	@Override
	public String toString() {
		return getClass() + "[" + render() + "]";
	}

	public List<FailureContextElement> getElements() {
		return elements;
	}

	/**
	 * @return A human-readable representation of this context.
	 */
	public String render() {
		StringJoiner contextJoiner = new StringJoiner( MESSAGES.contextSeparator() );
		for ( FailureContextElement element : elements ) {
			contextJoiner.add( element.render() );
		}
		return MESSAGES.contextPrefix() + contextJoiner.toString();
	}

}
