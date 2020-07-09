
package com.tlsconfig.validator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tlsconfig.exception.TLSException;
import com.tlsconfig.utils.Constants;
import com.tlsconfig.utils.ReturnCodes;

public final class MetaDataBuilder {

    private MetaDataBuilder() {
    }

    private static final Logger LOGGER = LogManager.getLogger(MetaDataBuilder.class);
    
    public static List<String> getServices() throws TLSException {
    	Set<String> services = getRegisteredServices();
    	List<String> staticallyRegisteredServices = getStaticallyRegisteredServices(Constants.METADATAFILEPATH);
		services.addAll(staticallyRegisteredServices);
		List<String> serviceList = new ArrayList<>();
		serviceList.addAll(services);
    	return serviceList;
    }

	private static List<String> getStaticallyRegisteredServices(String metaDataFilePath) throws TLSException {
		File metaDataFile = new File(metaDataFilePath);
        if (!metaDataFile.exists()) {
            LOGGER.error("Metadata File does not exist!");
            throw new TLSException("Metadata File does not exist !", ReturnCodes.FILE_NOT_FOUND.getValue());
        } else {
            try {
                List<String> serviceList = Files.readAllLines(Paths.get(metaDataFilePath),
                        StandardCharsets.UTF_8);
                if (serviceList.isEmpty()) {
                    LOGGER.error("No services added to the Metadata File");
                    throw new TLSException("Metadata File has no services added to it.",
                            ReturnCodes.EMPTY_METADATA_FILE.getValue());
                }
                return serviceList;
            } catch (IOException e) {
                String errorMessage = MessageFormat.format("Unable to read the metadata file: {0}",
                        e.getMessage());
                LOGGER.error(errorMessage, e);
                throw new TLSException(errorMessage, e, ReturnCodes.FILE_FORMAT_ERROR.getValue());
            }
        }
	}
	
	private static Set<String> getRegisteredServices() throws TLSException {
		File registryFile = new File(Constants.COMPONENT_REGISTRY_FILE);
		Set<String> services = new HashSet<>();
		if (registryFile.exists()) {
			try {
				services.addAll(Files.readAllLines(Paths.get(Constants.COMPONENT_REGISTRY_FILE), StandardCharsets.UTF_8));
			} catch (IOException e) {
				String errorMessage = MessageFormat.format("Unable to read the registry file: {0}. Error: {1}",
						Constants.COMPONENT_REGISTRY_FILE, e.getMessage());
				LOGGER.error(errorMessage, e);
				throw new TLSException(errorMessage, e, ReturnCodes.FILE_FORMAT_ERROR.getValue());
			}
		} else {
			LOGGER.info("Registry file doesn't exist. Assuming there are no registered services");
		}
        return services;
	}
}
