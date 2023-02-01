/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.hibernate.search.util.common.reporting.impl.CommonEventContextMessages;

/**
 * A sequence of {@link EventContextElement}.
 */
public final class EventContext {

	private static final CommonEventContextMessages MESSAGES = CommonEventContextMessages.INSTANCE;

	public static EventContext create(EventContextElement firstElement, EventContextElement... otherElements) {
		EventContext result = new EventContext( null, firstElement );
		for ( EventContextElement otherElement : otherElements ) {
			result = new EventContext( result, otherElement );
		}
		return result;
	}

	public static EventContext concat(EventContext first, EventContext... others) {
		EventContext result = first;
		for ( EventContext other : others ) {
			if ( result == null ) {
				result = other;
			}
			else if ( other != null ) {
				result = result.append( other );
			}
		}
		return result;
	}

	private final EventContext parent;
	private final EventContextElement appendedElement;

	private EventContext(EventContext parent, EventContextElement appendedElement) {
		this.parent = parent;
		this.appendedElement = appendedElement;
	}

	@Override
	public String toString() {
		return getClass() + "[" + render() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !getClass().equals( obj.getClass() ) ) {
			return false;
		}

		EventContext other = (EventContext) obj;

		return Objects.equals( parent, other.parent )
				&& appendedElement.equals( other.appendedElement );
	}

	@Override
	public int hashCode() {
		return Objects.hash( parent, appendedElement );
	}

	/**
	 * @return The elements of this context. Never empty, does not contain {@code null} values.
	 */
	public List<EventContextElement> elements() {
		List<EventContextElement> result = new ArrayList<>();
		addTo( result );
		return result;
	}

	/**
	 * @return A human-readable representation of this context.
	 * This representation may change without prior notice in new versions of Hibernate Search:
	 * callers should not try to parse it.
	 */
	public String render() {
		StringJoiner contextJoiner = new StringJoiner( MESSAGES.contextSeparator() );
		for ( EventContextElement element : elements() ) {
			contextJoiner.add( element.render() );
		}
		return contextJoiner.toString();
	}

	/**
	 * @return A human-readable representation of this context, with a "Context: " prefix.
	 * This representation may change without prior notice in new versions of Hibernate Search:
	 * callers should not try to parse it.
	 */
	public String renderWithPrefix() {
		return MESSAGES.contextPrefix() + render();
	}

	public EventContext append(EventContext other) {
		return other.appendTo( this );
	}

	private EventContext appendTo(EventContext other) {
		EventContext result = other;
		if ( parent != null ) {
			result = parent.appendTo( result );
		}
		result = new EventContext( result, appendedElement );
		return result;
	}

	private void addTo(List<EventContextElement> list) {
		if ( parent != null ) {
			parent.addTo( list );
		}
		list.add( appendedElement );
	}

}
