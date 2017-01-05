/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * @author Mincong Huang
 */
public class MassIndexerUtil {

	public static final Logger LOGGER = Logger.getLogger( MassIndexerUtil.class );

	public static String serialize(JobContextData ctxData)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		oos.writeObject( ctxData );
		oos.flush();
		oos.close();
		return Base64.getEncoder().encodeToString( baos.toByteArray() );
	}

	public static JobContextData deserializeJobContextData(String serialized)
			throws IOException, ClassNotFoundException {
		// TODO can NPE check be deleted?
		if ( serialized == null ) {
			LOGGER.warn( "JobContextData is null. Nothing to deserialize." );
			return null;
		}
		byte bytes[] = Base64.getDecoder().decode( serialized );
		ByteArrayInputStream bais = new ByteArrayInputStream( bytes );
		ObjectInputStream ois = new ObjectInputStream( bais );
		JobContextData jobContextData = (JobContextData) ois.readObject();
		ois.close();
		return jobContextData;
	}
}
