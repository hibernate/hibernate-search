/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.build.surefire;

import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelDecoder;
import org.apache.maven.surefire.spi.MasterProcessChannelProcessorFactory;

/**
 * This code was copied from the <a href="https://github.com/apache/maven-surefire">Maven Surefire project</a> to fix a bug.
 * Original class name: {@code org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelProcessorFactory}.
 * <p>
 * The fixed lines are surrounded by two comments lines that include "BEGIN FIX"/"END FIX".
 * <p>
 * PLEASE DON'T REFORMAT THIS FILE, so as to make pulling updates from the Surefire project easier.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 */
//CHECKSTYLE:OFF
public class FixedLegacyMasterProcessChannelProcessorFactory implements MasterProcessChannelProcessorFactory {
    @Override
    public boolean canUse( String channelConfig )
    {
        return channelConfig.startsWith( "pipe://" );
    }

    @Override
    public void connect( String channelConfig ) throws IOException
    {
        if ( !canUse( channelConfig ) )
        {
            throw new MalformedURLException( "Unknown chanel string " + channelConfig );
        }
    }

    @Override
    public MasterProcessChannelDecoder createDecoder()
    {
        return new LegacyMasterProcessChannelDecoder( newBufferedChannel( System.in ) );
    }

    @Override
    public MasterProcessChannelEncoder createEncoder()
    {
		// BEGIN FIX
		return new FixedLegacyMasterProcessChannelEncoder( newBufferedChannel( System.out ) );
		// END FIX
    }

    @Override
    public void close()
    {
    }
}