//$Id$
package org.hibernate.search;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Emmanuel Bernard
 */
public class Version {
	public static final String VERSION = "3.1.0.Beta1";

	static {
		Logger log = LoggerFactory.getLogger( Version.class );
		log.info( "Hibernate Search {}", VERSION );
	}

	public static void touch() {
	}
}
