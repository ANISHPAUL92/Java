package com.nokia.oss.securitymanagement.tlsconfig.validator;

import static com.nokia.oss.securitymanagement.tlsconfig.utils.BundleManager.getResourceString;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nokia.oss.securitymanagement.tlsconfig.dto.SSHExecutionStatusDTO;
import com.nokia.oss.securitymanagement.tlsconfig.exception.TLSException;
import com.nokia.oss.securitymanagement.tlsconfig.utils.Constants;
import com.nokia.oss.securitymanagement.tlsconfig.utils.PropReader;
import com.nokia.oss.securitymanagement.tlsconfig.utils.SSHAdapter;

public class HealthChecker {

	private static final Logger LOGGER = LogManager.getLogger(HealthChecker.class);

	public Set<String> performHealthCheck(Set<String> listOfAllServices) throws TLSException {
		Properties props = PropReader.getInstance().getProperties();
		long timeOut = Long.parseLong(props.getProperty("SERVICE_HEALTHCHECK_TIMEOUT", "120"));
		Set<String> failedHealthCheckServices = new HashSet<>();
		Set<String> healthCheckErrorMessages = new HashSet<>();
		LOGGER.traceEntry("Checking the health status for the services {} . Timeout defined per service is {}",
				listOfAllServices, timeOut);
		SSHAdapter sshAdapter = new SSHAdapter(Constants.ROOT_USER, Constants.LOCALHOST);
		try {
			for (String serviceName : listOfAllServices) {
				String command = MessageFormat.format(Constants.SMANAGER_PL_CMD_FRMT, serviceName);
				LOGGER.debug("Starting to execute command: {}", command);
				SSHExecutionStatusDTO statusDTO = null;
				try {
					statusDTO = sshAdapter.executeCommand(command, timeOut);
				} catch (TLSException tlse) {
					LOGGER.error("Exception caught while executing the command with error code {} and cause {}",
							tlse, tlse.getCause());
					failedHealthCheckServices.add(serviceName);
					continue;
				}
				if (statusDTO == null || statusDTO.getCommandOutput() == null
						|| statusDTO.getRetCode() != Constants.SUCCESS_RET_CODE) {
					LOGGER.error("Unable to get the command output for service {}. StatusDTO is {} ", serviceName,
							statusDTO);
					failedHealthCheckServices.add(serviceName);
					continue;
				}
				if (!isServiceHealthy(serviceName, statusDTO)) {
					failedHealthCheckServices.add(serviceName);
				}
			}
			if (!failedHealthCheckServices.isEmpty()) {
				LOGGER.error("Health check failed for the services {} ", failedHealthCheckServices);
				String failedHealthCheckMessage = MessageFormat.format(getResourceString("HEALTH_CHECK_FAIL_FOR_SERVICES"),
						failedHealthCheckServices);
				LOGGER.info("message is " + failedHealthCheckMessage);
				healthCheckErrorMessages.add(failedHealthCheckMessage);
			} else {
				LOGGER.info("Health check passed for all the services {}", listOfAllServices);
			}
		} finally {
			sshAdapter.close();
		}
		LOGGER.traceExit("completed checking the health status for the services");
		return healthCheckErrorMessages;
	}

	private boolean isServiceHealthy(String serviceName, SSHExecutionStatusDTO statusDTO) {
		LOGGER.traceEntry("Checking the health status for the service {}. StatusDTO is {}", serviceName, statusDTO);
		String commandOutput = statusDTO.getCommandOutput();
		String[] outputLines = commandOutput.split(Constants.LINE_SEPARATOR_PATTERN);
		boolean isSmanagerOutputEmpty = true;
		for (String outputLine : outputLines) {
			if (outputLine.trim().isEmpty()) {
				continue;
			}
			isSmanagerOutputEmpty = false;
			String[] tokens = outputLine.split(Constants.SERVICE_VM_SEPARATOR);
			if (tokens.length != Constants.SMANAGER_OUTPUT_TOKEN_LENGTH) {
				LOGGER.error("unexpected output line i.e. {} returned from server monitoring for service {} ",
						outputLine, serviceName);
				return false;
			}
			String serviceInstanceName = tokens[Constants.SM_SERVICE_INSTANCE_NAME];
			String nodeName = tokens[Constants.SM_NODENAME];
			String serviceStatus = tokens[Constants.SM_SERVICESTATUS];
			LOGGER.debug("checking health status for the output line {} ", outputLine);
			if (serviceNodeCheck(serviceName, serviceInstanceName, nodeName)) {
				if (!(serviceStatus.equals(Constants.STARTED_STATE))) {
					LOGGER.error("Health check failed for service {}. Errorcode is {} and command output is {} ",
							serviceName, statusDTO.getRetCode(), statusDTO.getCommandOutput());
					return false;
				} else {
					LOGGER.debug("health check is fine for service {} running under node {}", serviceInstanceName,
							nodeName);
				}
			} else {
				LOGGER.info(
						"skipping health check for {} service on {} node as it is not the relevant service to check for",
						serviceInstanceName, nodeName);
			}
		}
		if (isSmanagerOutputEmpty) {
			LOGGER.error("smanager output is empty for service {}", serviceName);
			return false;
		}
		LOGGER.traceExit("Completed checking the health status for the service {}", serviceName);
		return true;
	}

	private boolean serviceNodeCheck(String serviceName, String serviceInstanceName, String nodeName) {
		return serviceName.concat(Constants.HYPHEN).concat(nodeName).equals(serviceInstanceName)
				|| serviceName.equals(serviceInstanceName);
	}
}
