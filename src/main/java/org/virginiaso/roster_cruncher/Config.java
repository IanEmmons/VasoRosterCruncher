package org.virginiaso.roster_cruncher;

import java.io.File;
import java.util.Properties;

import jakarta.mail.Session;

public class Config {
	private static class ConfigHolder {
		private static final Config INSTANCE = new Config();
	}

	private static final String CONFIGURATION_RESOURCE = "configuration.properties";

	private final Properties props;
	private final String portalAppName;
	private final String portalUser;
	private final String portalPassword;
	private final String portalApplicationId;
	private final File portalReportDir;
	private final String portalPermissionUrl;
	private final File masterReportFile;
	private final boolean sendReports;
	private final String mailFromAddr;
	private final String mailUserName;
	private final String mailPassword;
	private final String mailSubject;
	private final String mailBodyResourceName;
	private final String mailMtgSummonsUrl;
	private final String scilympiadSiteName;
	private final File scilympiadReportDir;
	private final String[] scilympiadSuffixes;

	/**
	 * Get the singleton instance of Config. This follows the "lazy initialization
	 * holder class" idiom for lazy initialization of a static field. See Item 83 of
	 * Effective Java, Third Edition, by Joshua Bloch for details.
	 *
	 * @return the instance
	 */
	public static Config inst() {
		return ConfigHolder.INSTANCE;
	}

	private Config() {
		props = Util.loadPropertiesFromResource(CONFIGURATION_RESOURCE);
		portalAppName = props.getProperty("portal.application.name");
		portalUser = props.getProperty("portal.user");
		portalPassword = props.getProperty("portal.password");
		var appNamePropKey = "portal.%1$s.application.id".formatted(portalAppName);
		portalApplicationId = props.getProperty(appNamePropKey);
		portalReportDir = Util.parseFileArgument(props, "portal.report.dir");
		masterReportFile = Util.parseFileArgument(props, "master.report.file");
		sendReports = Boolean.parseBoolean(props.getProperty("send.reports", "false"));
		mailFromAddr = props.getProperty("mail.from");
		mailUserName = props.getProperty("mail.user");
		mailPassword = props.getProperty("mail.password");
		mailSubject = props.getProperty("mail.subject");
		mailBodyResourceName = props.getProperty("mail.body.resource.name");
		mailMtgSummonsUrl = props.getProperty("mail.meetingSummonsUrl");
		var permissionUrlPropKey = "portal.%1$s.permission.url".formatted(portalAppName);
		portalPermissionUrl = props.getProperty(permissionUrlPropKey);
		scilympiadSiteName = props.getProperty("scilympiad.site");
		scilympiadReportDir = Util.parseFileArgument(props, "scilympiad.report.dir");
		var siteNamePropKey = "scilympiad.%1$s.suffixes".formatted(scilympiadSiteName);
		scilympiadSuffixes = props.getProperty(siteNamePropKey, "").split(",");
	}

	public String getPortalAppName() {
		return portalAppName;
	}

	public String getPortalUser() {
		return portalUser;
	}

	public String getPortalPassword() {
		return portalPassword;
	}

	public String getPortalApplicationId() {
		return portalApplicationId;
	}

	public File getPortalReportDir() {
		return portalReportDir;
	}

	public String getPortalScene(String reportName) {
		return props.getProperty("portal.%1$s.%2$s.scene".formatted(portalAppName, reportName));
	}

	public String getPortalView(String reportName) {
		return props.getProperty("portal.%1$s.%2$s.view".formatted(portalAppName, reportName));
	}

	public String getPortalPermissionUrl() {
		return portalPermissionUrl;
	}

	public File getMasterReportFile() {
		return masterReportFile;
	}

	public boolean sendReports() {
		return sendReports;
	}

	public Session getEmailSession() {
		return Session.getDefaultInstance(props);
	}

	public String getMailFromAddr() {
		return mailFromAddr;
	}

	public String getMailUserName() {
		return mailUserName;
	}

	public String getMailPassword() {
		return mailPassword;
	}

	public String getMailSubject() {
		return mailSubject;
	}

	public String getMailBodyResourceName() {
		return mailBodyResourceName;
	}

	public String getMailMtgSummonsUrl() {
		return mailMtgSummonsUrl;
	}

	public String getScilympiadSiteName() {
		return scilympiadSiteName;
	}

	public File getScilympiadReportDir() {
		return scilympiadReportDir;
	}

	public String[] getScilympiadSuffixes() {
		return scilympiadSuffixes;
	}
}
