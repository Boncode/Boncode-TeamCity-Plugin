package nl.ijsberg.analysis.server.buildserver;

import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.util.collection.ArraySupport;
import org.ijsberg.iglu.util.io.FSFileCollection;
import org.ijsberg.iglu.util.io.FileFilterRuleSet;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.io.ZipFileStreamProvider;
import org.ijsberg.iglu.util.misc.StringSupport;
import org.ijsberg.iglu.util.properties.PropertiesSupport;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 */
public class BuildServerToMonitorLink {

	private String customerId;
	private String projectId;
	private final String monitorUploadDirectory;
	private final String analysisProperties;
	private final Properties properties = new Properties();
	
	private Logger logger;

	public static final String SNAPSHOT_TIMESTAMP_FORMAT = "yyyyMMdd_HH_mm";

	public BuildServerToMonitorLink(String analysisProperties, String monitorUploadDirectory, Logger logger) {
		this.logger = logger;
		this.monitorUploadDirectory = monitorUploadDirectory;
		this.analysisProperties = analysisProperties;
		try {
			loadProperties(analysisProperties);
		} catch (IOException e) {
			throw new ConfigurationException("properties in " + analysisProperties + " cannot be loaded");
		}
	}

	public static void throwIfPropertiesNotOk(String analysisProperties, String monitorUploadDirectory, String sourceRoot) {
		ValidationResult checkedAnalysisProperties = checkAnalysisProperties(analysisProperties);
		if(!checkedAnalysisProperties.isOk()) {
			throw new ConfigurationException(checkedAnalysisProperties.getLastMessage());
		}
		ValidationResult checkedUploadDirectory = checkMonitorUploadDirectory(monitorUploadDirectory);
		if(!checkedAnalysisProperties.isOk()) {
			throw new ConfigurationException(checkedUploadDirectory.getLastMessage());
		}
		ValidationResult checkedSourceRoot = checkSourceRoot(monitorUploadDirectory);
		if(!checkedSourceRoot.isOk()) {
			throw new ConfigurationException(checkedUploadDirectory.getLastMessage());
		}
	}

	private void loadProperties(String analysisProperties) throws IOException {
		InputStream inputStream = new FileInputStream(new File(analysisProperties));
		properties.load(inputStream);
		this.projectId = properties.getProperty("projectName");
		this.customerId = properties.getProperty("customerName");
		System.out.println(new LogEntry(Level.VERBOSE, "loaded properties for customer "
				+ this.customerId + ", project " + this.projectId));
		inputStream.close();
	}

	/**
	 * We'll use this from the <tt>config.jelly</tt>.
	 */
	public String getCustomerId() {
		return customerId;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getMonitorUploadDirectory() {
		return monitorUploadDirectory;
	}

	public String getAnalysisProperties() {
		return analysisProperties;
	}

	private PrintStream logStream = System.out;

	public boolean perform(String workSpacePath) {

		// This is where you 'build' the project.
		// This also shows how you can consult the global configuration of the builder
		// reload properties on-the-fly
		try {
			loadProperties(analysisProperties);
		} catch (IOException e) {
			logger.log(new LogEntry("ERROR: unable to reload properties " + analysisProperties, e));
			return false;
		}

		File uploadDirectory = new File(monitorUploadDirectory);
		if(!uploadDirectory.exists()) {
			logger.log(new LogEntry("ERROR: upload directory " + uploadDirectory.getAbsolutePath() + " does not exist or is not accessible"));
			return false;
		}
		if(!uploadDirectory.isDirectory()) {
			logger.log(new LogEntry("ERROR: " + uploadDirectory.getAbsolutePath() + " is not a directory"));
			return false;
		}
		//String workSpacePath = build.getWorkspace().getRemote();
		logger.log(new LogEntry("zipping sources from " + workSpacePath + " to " + uploadDirectory.getAbsolutePath()));

		String destfileName = monitorUploadDirectory + "/" + getSnapshotZipfileName(customerId, projectId, new Date());
		try {
			zipSources(workSpacePath, destfileName);
		} catch (IOException e) {
			logger.log(new LogEntry("exception encountered during zip process with message: " + e.getMessage()));
			e.printStackTrace();
			return false;
		}
		logger.log(new LogEntry("DONE ... created snapshot " + destfileName));

		logStream = System.out;

		return true;
	}


	private void zipSources(String workSpacePath, String destfileName) throws IOException {
		ZipFileStreamProvider zipFileStreamProvider = new ZipFileStreamProvider(destfileName);
		List<String> languages = StringSupport.split(properties.getProperty("languages"));

		for(String language : languages) {
			logger.log(new LogEntry("zipping sources for language " + language));
			Properties languageProperties = PropertiesSupport.getSubsection(properties, language);
			FileFilterRuleSet fileFilterRuleSet = configureFileFilter(PropertiesSupport.getSubsection(languageProperties, "fileFilter"));
			copyFilesToZip(workSpacePath, zipFileStreamProvider, fileFilterRuleSet);
			Properties testFileFilterProperties = PropertiesSupport.getSubsection(languageProperties, "testFileFilter");
			if(testFileFilterProperties != null && !testFileFilterProperties.isEmpty()){
				FileFilterRuleSet testFileFilterRuleSet = configureFileFilter(testFileFilterProperties);
				logger.log(new LogEntry("zipping TEST sources for language " + language));
				copyFilesToZip(workSpacePath, zipFileStreamProvider, testFileFilterRuleSet);
			}
		}
		zipFileStreamProvider.close();
		FileSupport.createFile(destfileName + ".DONE");
	}

	private static void copyFilesToZip(String workSpacePath, ZipFileStreamProvider zipFileStreamProvider, FileFilterRuleSet fileFilterRuleSet) throws IOException {
		FSFileCollection fsFileCollection = new FSFileCollection(workSpacePath, fileFilterRuleSet);
		for(String fileName : fsFileCollection.getFileNames()) {
			OutputStream outputStream = zipFileStreamProvider.createOutputStream(fileName);
			File fileInCollection = fsFileCollection.getActualFileByName(fileName);
			FileSupport.copyFileResource(fileInCollection.getAbsolutePath(), outputStream);
			zipFileStreamProvider.closeCurrentStream();
		}
	}

	public static FileFilterRuleSet configureFileFilter(Properties fileFilterProperties) {

		FileFilterRuleSet retval = new FileFilterRuleSet().setIncludeFilesWithNameMask("*.*");

		retval.setIncludeFilesWithNameMask(ArraySupport.format(getSettingArray(fileFilterProperties, "includeFilesWithName"), "|"));
		retval.setExcludeFilesWithNameMask(ArraySupport.format(getSettingArray(fileFilterProperties, "excludeFilesWithName"), "|"));
		retval.setIncludeFilesContainingText(getSettingArray(fileFilterProperties, "includeFilesContainingText"));
		retval.setExcludeFilesContainingText(getSettingArray(fileFilterProperties, "excludeFilesContainingText"));

		return retval.clone();
	}


	public static String[] getSettingArray(Properties properties, String key) {
		String value = properties.getProperty(key);
		if(value == null || value.isEmpty()) {
			return new String[]{};
		}
		return StringSupport.split(value).toArray(new String[]{});
	}



	public static String getSnapshotZipfileName(String customerId, String projectId, Date snapshotTimestamp) {
		return customerId + "." +
				projectId + "." + new SimpleDateFormat(SNAPSHOT_TIMESTAMP_FORMAT).format(snapshotTimestamp) + ".zip";
	}

	public static final String ANALYSIS_PROPERTIES_FILENAME = "analysisProperties";

	public static ValidationResult checkAnalysisProperties(String value) {
		//TODO value may be null
		if (value.length() == 0)
			return ValidationResult.notOk(ANALYSIS_PROPERTIES_FILENAME, "Please provide a path to the analysis properties file");
		File file = new File(value);
		if(!file.exists()) {
			return ValidationResult.notOk(ANALYSIS_PROPERTIES_FILENAME, "File " + value + " does not exist or is not accessible");
		}
		if(file.isDirectory()) {
			return ValidationResult.notOk(ANALYSIS_PROPERTIES_FILENAME, value + " is a directory");
		}
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(value)));
		} catch (IOException e) {
			return ValidationResult.notOk(ANALYSIS_PROPERTIES_FILENAME, "properties " + value + " cannot be loaded with message: " + e.getMessage());
		}

		if(!properties.containsKey("customerName")) {
			return ValidationResult.notOk(ANALYSIS_PROPERTIES_FILENAME, "Properties file " + value + " does not contain property customerName");
		}
		if(!properties.containsKey("projectName")) {
			return ValidationResult.notOk(ANALYSIS_PROPERTIES_FILENAME, "Properties file " + value + " does not contain property projectName");
		}
		return ValidationResult.ok(ANALYSIS_PROPERTIES_FILENAME);
	}

	public static final String SOURCE_ROOT = "sourceRoot";
	public static final String MONITOR_UPLOAD_DIRECTORY = "monitorUploadDirectory";

	public static ValidationResult checkMonitorUploadDirectory(String value) {
		return checkDirectory(MONITOR_UPLOAD_DIRECTORY, value);
	}

	public static ValidationResult checkSourceRoot(String value) {
		return checkDirectory(SOURCE_ROOT, value);
	}

	public static ValidationResult checkDirectory(String propertyName, String value) {
		if (value.length() == 0)
			return ValidationResult.notOk(propertyName, propertyName + " is empty please provide a valid path");
		File file = new File(value);
		if(!file.exists()) {
			return ValidationResult.notOk(propertyName, "File " + value + " does not exist or is not accessible");
		}
		if(!file.isDirectory()) {
			return ValidationResult.notOk(propertyName, value + " is not a directory");
		}
		return ValidationResult.ok(propertyName);
	}

}
