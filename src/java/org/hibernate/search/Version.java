//$Id$
package org.hibernate.search;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Emmanuel Bernard
 */
public class Version {
	public static final String VERSION = "3.0.1.GA"; // + new Date();
	private static Log log = LogFactory.getLog( Version.class );

	static {
		log.info( "Hibernate Search " + VERSION );
	}

	public static void touch() {
	}
}
