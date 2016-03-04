package org.hibernate.search.backend.triggers.impl;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.search.db.events.impl.AnnotationEventModelParser;
import org.hibernate.search.db.events.impl.EventModelInfo;
import org.hibernate.search.db.events.impl.EventModelParser;

/**
 * This EventModelParser should be used when Hibernate ORM
 * is available we have more information about how stuff
 * is persisted available compared to plain JPA
 *
 * @author Martin Braun
 */
public class ORMEventModelParser implements EventModelParser {

	//FIXME: implement this, will need some help from ORM devs

	private AnnotationEventModelParser defaultParser = new AnnotationEventModelParser();
	private final SessionFactory sessionFactory;

	public ORMEventModelParser(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public List<EventModelInfo> parse(List<Class<?>> updateClasses) {
		throw new UnsupportedOperationException( "not implemented, yet!" );
	}

}
