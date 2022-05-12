/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.EventContextElement;
import org.hibernate.search.util.common.SearchException;

public final class FailureReportUtils {

	private FailureReportUtils() {
	}

	/**
	 * @param first The first part of the expected context.
	 * @param others The other parts of the expected context, if any. To be concatenated to the first part.
	 * @return A consumer representing an assertion to be passed as a parameter to
	 * {@link org.assertj.core.api.AbstractThrowableAssert#satisfies(Consumer[])}.
	 */
	public static <T extends Throwable> Consumer<T> hasContext(EventContext first, EventContext... others) {
		return hasContext(
				EventContext.concat( first, others ).elements().toArray( new EventContextElement[] { } )
		);
	}

	/**
	 * @param contextElements The expect context elements, in order.
	 * @return A consumer representing an assertion to be passed as a parameter to
	 * {@link org.assertj.core.api.AbstractThrowableAssert#satisfies(Consumer[])}.
	 */
	public static <T extends Throwable> Consumer<T> hasContext(EventContextElement... contextElements) {
		return throwable -> {
			assertThat( throwable )
					.isInstanceOf( SearchException.class );
			EventContext actualContext = ( (SearchException) throwable ).context();
			assertThat( actualContext ).as( "throwable.getContext()" ).isNotNull();
			assertThat( actualContext.elements() )
					.containsExactly( contextElements );
			String renderedContextElements = Arrays.stream( contextElements ).map( EventContextElement::render )
					.collect( Collectors.joining( ", " ) );
			assertThat( throwable.getMessage() )
					.endsWith( "Context: " + renderedContextElements );
		};
	}

	public static Consumer<Throwable> hasCauseWithContext(EventContext first, EventContext ... others) {
		Consumer<Throwable> delegate = hasContext( first, others );
		return e -> delegate.accept( e.getCause() );

	}

	public static FailureReportChecker hasFailureReport() {
		return new FailureReportChecker();
	}

}
