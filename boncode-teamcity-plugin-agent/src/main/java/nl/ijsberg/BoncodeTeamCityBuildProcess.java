package nl.ijsberg;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import nl.ijsberg.analysis.server.buildserver.AnalysisPropertiesLoader;
import nl.ijsberg.analysis.server.buildserver.BuildServerToMonitorLink;
import nl.ijsberg.analysis.server.buildserver.ValidationResult;
import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.util.properties.PropertiesSupport;
import org.ijsberg.iglu.util.reflection.ReflectionSupport;
import org.jetbrains.annotations.NotNull;
import java.util.Properties;

import java.util.Map;

/**
 */
public class BoncodeTeamCityBuildProcess implements BuildProcess {

	private BuildRunnerContext context;

	private String monitorUploadDirectory;
    private String monitorDownloadDirectory;

    private String analysisPropertiesFileName;
	private String sourceRoot;
	private String checkoutDir;
	private AnalysisPropertiesLoader analysisPropertiesLoader;
	private String alternativePropertiesInWorkingDir;

	private Logger logger;
	private BuildServerToMonitorLink buildServerToMonitorLink;

	private boolean skipBuild = false;

	public BoncodeTeamCityBuildProcess(BuildRunnerContext context, Logger logger) {

		this.context = context;
		this.logger = logger;
		Map<String, String> configParameters = context.getConfigParameters();
		Map<String, String> buildParameters = context.getConfigParameters();
		logger.log(new LogEntry("config parameters: " + configParameters));
		logger.log(new LogEntry("build parameters: " + buildParameters));
		logger.log(new LogEntry("Checkout dir: " + context.getBuild().getCheckoutDirectory()));
		logger.log(new LogEntry("Project name: " + context.getBuild().getProjectName()));
		logger.log(new LogEntry("Context name: " + context.getName()));

		checkoutDir = context.getBuild().getCheckoutDirectory().getAbsolutePath();
		sourceRoot = checkoutDir + "/" + buildParameters.get(BuildServerToMonitorLink.SOURCE_ROOT);
		monitorUploadDirectory = buildParameters.get(BuildServerToMonitorLink.MONITOR_UPLOAD_DIRECTORY);
        monitorDownloadDirectory = buildParameters.get(BuildServerToMonitorLink.MONITOR_DOWNLOAD_DIRECTORY);

		String analysisPropertiesLoaderClassName = buildParameters.get(BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_LOADER);
		if(analysisPropertiesLoaderClassName != null) {
			try {
				analysisPropertiesLoader = (AnalysisPropertiesLoader)ReflectionSupport.instantiateClass(analysisPropertiesLoaderClassName);
			} catch (InstantiationException e) {
				throw new ConfigurationException("cannot instantiate class found in property " + BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_LOADER, e);
			}
			alternativePropertiesInWorkingDir = buildParameters.get(BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_IN_WORKING_DIR);
			ValidationResult result = BuildServerToMonitorLink.checkFile(
					BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_IN_WORKING_DIR,
					checkoutDir + "/" + alternativePropertiesInWorkingDir,
					false);
			if(!result.isOk()) {
//				throw new ConfigurationException("please provide a valid file with alternative properties: " + result.getLastMessage());
				logger.log(new LogEntry(Level.CRITICAL, "please provide a valid file with alternative properties: " + result.getLastMessage()));
				skipBuild = true;
				return;
			}
		} else {
			logger.log(new LogEntry("no value found for property " + BuildServerToMonitorLink.ALTERNATIVE_PROPERTIES_LOADER + " using standard properties loader"));
		}
        analysisPropertiesFileName = buildParameters.get(BuildServerToMonitorLink.ANALYSIS_PROPERTIES_FILENAME);
		BuildServerToMonitorLink.throwIfPropertiesNotOk(
				analysisPropertiesFileName,
				monitorUploadDirectory,
				sourceRoot);
		Properties properties = loadProperties();
		logger.log(new LogEntry("performing analysis with properties: " + properties));
		buildServerToMonitorLink = new BuildServerToMonitorLink(properties, monitorUploadDirectory, monitorDownloadDirectory, logger);
	}

	private Properties loadProperties() {
		if(analysisPropertiesLoader != null) {
			logger.log(new LogEntry("loading alternative properties using " + analysisPropertiesLoader.getClass()));
			Properties properties =  analysisPropertiesLoader.load(analysisPropertiesFileName, checkoutDir + "/" + alternativePropertiesInWorkingDir);
			return properties;
		}
		return PropertiesSupport.loadProperties(analysisPropertiesFileName);
	}

	public void start() throws RunBuildException {
		if(skipBuild) {
			logger.log(new LogEntry("Skipping Boncode analysis"));
			return;
		}
		logger.log(new LogEntry("Starting Boncode analysis"));
        boolean result = buildServerToMonitorLink.perform(sourceRoot);
        if(result){
            this.isFinished();
        } else {
            this.isInterrupted();
        }
	}

	public boolean isInterrupted() {
		logger.log(new LogEntry("isInterrupted invoked"));
		return false;
	}

	public boolean isFinished() {
		logger.log(new LogEntry("isFinished invoked"));
		return false;
	}

	public void interrupt() {
		logger.log(new LogEntry("interrupt invoked"));
	}

	@NotNull
	public BuildFinishedStatus waitFor() throws RunBuildException {
		logger.log(new LogEntry("waitFor invoked"));
		return BuildFinishedStatus.FINISHED_SUCCESS;
	}

}
