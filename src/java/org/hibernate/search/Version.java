//$Id$
package org.hibernate.search;

import java.util.Date;

import org.slf4j.Logger;

import org.hibernate.search.util.LoggerFactory;


/**
 * @author Emmanuel Bernard
 */
public class Version {
	public static final String VERSION = "3.1.0.SNAPSHOT" + new Date();

	private static final Logger log = LoggerFactory.make();

	static {
		log.info( "Hibernate Search {}", VERSION );
	}

	public static void touch() {
	}
}
