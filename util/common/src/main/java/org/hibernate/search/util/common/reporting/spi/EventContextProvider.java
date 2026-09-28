package org.hibernate.search.util.common.reporting.spi;

import org.hibernate.search.util.common.reporting.EventContext;

public interface EventContextProvider {

	EventContext eventContext();

}
