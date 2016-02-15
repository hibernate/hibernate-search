package org.hibernate.search.genericjpa.db.events.eclipselink.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.search.genericjpa.entity.ReusableEntityProvider;

/**
 * Created by Martin on 28.07.2015.
 */
public class DummyReusableEntityProvider implements ReusableEntityProvider {

	@Override
	public void close() {
		throw new AssertionFailure( "should not have been used" );
	}

	@Override
	public void open() {
		throw new AssertionFailure( "should not have been used" );
	}

	@Override
	public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
		throw new AssertionFailure( "should not have been used" );
	}

	@Override
	public List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
		throw new AssertionFailure( "should not have been used" );
	}

}
