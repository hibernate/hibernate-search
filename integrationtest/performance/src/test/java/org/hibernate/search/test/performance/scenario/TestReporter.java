/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.Version;
import org.hibernate.search.test.performance.task.AbstractTask;
import org.hibernate.search.test.performance.util.CheckerLuceneIndex;
import org.hibernate.search.test.performance.util.CheckerUncaughtExceptions;
import org.hibernate.search.test.performance.util.TargetDirHelper;
import org.hibernate.search.testsupport.TestConstants;

import static org.apache.commons.lang.StringUtils.leftPad;
import static org.apache.commons.lang.StringUtils.rightPad;
import static org.hibernate.search.test.performance.TestRunnerArquillian.RUNNER_PROPERTIES;
import static org.hibernate.search.test.performance.TestRunnerArquillian.TARGET_DIR_KEY;
import static org.hibernate.search.test.performance.scenario.TestContext.ASSERT_QUERY_RESULTS;
import static org.hibernate.search.test.performance.scenario.TestContext.CHECK_INDEX_STATE;
import static org.hibernate.search.test.performance.scenario.TestContext.MEASURE_MEMORY;
import static org.hibernate.search.test.performance.scenario.TestContext.MEASURE_TASK_TIME;
import static org.hibernate.search.test.performance.scenario.TestContext.THREADS_COUNT;
import static org.hibernate.search.test.performance.scenario.TestContext.VERBOSE;
import static org.hibernate.search.test.performance.util.Util.runGarbageCollectorAndWait;

/**
 * @author Tomas Hradec
 */
public class TestReporter {

	private TestReporter() {
	}

	public static void printReport(TestContext ctx) throws UnsupportedEncodingException, IOException {
		PrintStream outStream = createOutputStream( ctx.scenario.getClass().getSimpleName() );
		PrintWriter outWriter = new PrintWriter(
				new BufferedWriter( new OutputStreamWriter( outStream, "UTF-8" ) ), false );

		outWriter.println( "==================================================================" );
		outWriter.println( "HIBERNATE SEARCH PERFORMANCE TEST REPORT" );
		printSummary( ctx, outWriter );
		printTaskInfo( ctx, outWriter );
		printEnvInfo( ctx, outWriter );
		outWriter.println( "==================================================================" );
		outWriter.flush();

		CheckerLuceneIndex.printIndexReport( ctx, outStream );
		CheckerUncaughtExceptions.printUncaughtExceptions( ctx, outWriter );

		outWriter.close();
		outStream.close();
	}

	private static void printSummary(TestContext ctx, PrintWriter out) {
		long freeMemory2 = -1;
		long totalMemory2 = -1;
		if ( MEASURE_MEMORY ) {
			runGarbageCollectorAndWait();
			freeMemory2 = Runtime.getRuntime().freeMemory();
			totalMemory2 = Runtime.getRuntime().totalMemory();
		}

		long totalNanos = ctx.stopTime - ctx.startTime;
		long totalMilis = TimeUnit.MILLISECONDS.convert( totalNanos, TimeUnit.NANOSECONDS );

		out.println( "" );
		out.println( "SUMMARY" );
		out.println( "    Name   : " + ctx.scenario.getClass().getSimpleName() );
		out.println( "    Date   : " + DateFormatUtils.format( new Date(), "yyyy-MM-dd HH:mm" ) );
		out.println( "" );
		out.println( "    Measured time (HH:mm:ss.SSS)" );
		out.println( "        MEASURED TASKS : " + DurationFormatUtils.formatDuration( totalMilis, "HH:mm:ss.SSS" ) );
		out.println( "        init database  : " + DurationFormatUtils.formatDuration( ctx.scenario.initDatabaseStopWatch.getTime(), "HH:mm:ss.SSS" ) );
		out.println( "        init index     : " + DurationFormatUtils.formatDuration( ctx.scenario.initIndexStopWatch.getTime(), "HH:mm:ss.SSS" ) );
		out.println( "        warmup phase   : " + DurationFormatUtils.formatDuration( ctx.scenario.warmupStopWatch.getTime(), "HH:mm:ss.SSS" ) );

		if ( MEASURE_MEMORY ) {
			out.println( "" );
			out.println( "    Memory usage (total-free):" );
			out.println( "        before : " + toMB( ctx.totalMemory - ctx.freeMemory ) );
			out.println( "        after  : " + toMB( totalMemory2 - freeMemory2 ) );
		}
	}

	private static void printTaskInfo(TestContext ctx, PrintWriter out) {
		out.println( "" );
		out.println( "TASKS" );
		for ( AbstractTask task : ctx.tasks ) {
			String taskTotalTime = "n/a";
			String taskAverageTime = "n/a";
			if ( MEASURE_TASK_TIME ) {
				long taskTotalMilis = TimeUnit.MILLISECONDS.convert( task.getTimerValue(), TimeUnit.NANOSECONDS );
				long taskAverageMilis = taskTotalMilis / task.getCounterValue();
				taskTotalTime = DurationFormatUtils.formatDuration( taskTotalMilis, "mm:ss.SSS" );
				taskAverageTime = DurationFormatUtils.formatDuration( taskAverageMilis, "mm:ss.SSS" );
			}
			out.println( "    "
					+ leftPad( task.getCounterValue() + "x ", 5 )
					+ rightPad( task.getClass().getSimpleName(), 35 )
					+ " | sum " + taskTotalTime
					+ " | avg " + taskAverageTime );
		}
	}

	private static void printEnvInfo(TestContext ctx, PrintWriter out) {
		out.println( "" );
		out.println( "TEST CONFIGURATION" );
		out.println( "    measure performance : " + TestConstants.arePerformanceTestsEnabled() );
		out.println( "    threads             : " + THREADS_COUNT );
		out.println( "    measured cycles     : " + ctx.scenario.measuredCyclesCount );
		out.println( "    warmup cycles       : " + ctx.scenario.warmupCyclesCount );
		out.println( "    initial book count  : " + ctx.scenario.initialBookCount );
		out.println( "    initial autor count : " + ctx.scenario.initialAutorCount );
		out.println( "    verbose             : " + VERBOSE );
		out.println( "    measure memory      : " + MEASURE_MEMORY );
		out.println( "    measure task time   : " + MEASURE_TASK_TIME );
		out.println( "    check query results : " + ASSERT_QUERY_RESULTS );
		out.println( "    check index state   : " + CHECK_INDEX_STATE );

		out.println( "" );
		out.println( "HIBERNATE SEARCH PROPERTIES" );
		Map<String, Object> properties = ( (SessionFactoryImplementor) ctx.sf ).getProperties();
		for ( Entry<String, Object> e : properties.entrySet() ) {
			if ( e.getKey().toString().startsWith( "hibernate.search" ) ) {
				out.println( "    " + e.getKey() + " = " + e.getValue() );
			}
		}

		out.println( "" );
		out.println( "VERSIONS" );
		out.println( "    org.hibernate.search : " + Version.getVersionString() );
		out.println( "    org.hibernate        : " + org.hibernate.Version.getVersionString() );
		out.println( "" );
	}

	private static String toMB(long bytes) {
		long megabytes = bytes / 1000 / 1000;
		return megabytes + "MB";
	}

	private static PrintStream createOutputStream(String testScenarioName) {
		try {
			Path targetDir = getTargetDir();
			String reportFileName = "report-" + testScenarioName + "-" + DateFormatUtils.format( new Date(), "yyyy-MM-dd-HH'h'mm'm'" ) + ".txt";
			Path reportFile = targetDir.resolve( reportFileName );

			final OutputStream std = System.out;
			final OutputStream file = new PrintStream( new FileOutputStream( reportFile.toFile() ), true, "UTF-8" );
			final OutputStream stream = new OutputStream() {

				@Override
				public void write(int b) throws IOException {
					std.write( b );
					file.write( b );
				}

				@Override
				public void flush() throws IOException {
					std.flush();
					file.flush();
				}

				@Override
				public void close() throws IOException {
					file.close();
				}
			};
			return new PrintStream( stream, false, "UTF-8" );
		}
		catch (FileNotFoundException | UnsupportedEncodingException e) {
			throw new RuntimeException( e );
		}
	}

	private static Path getTargetDir() {
		InputStream runnerPropertiesStream = TestReporter.class.getResourceAsStream( "/" + RUNNER_PROPERTIES );
		if ( runnerPropertiesStream != null ) {
			Properties runnerProperties = new Properties();
			try {
				runnerProperties.load( runnerPropertiesStream );
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
			return Paths.get( runnerProperties.getProperty( TARGET_DIR_KEY ) );
		}
		else {
			return TargetDirHelper.getTargetDir();
		}
	}

}
