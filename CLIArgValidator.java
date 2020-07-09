
package com.tlsconfig.validator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import com.tlsconfig.dto.OperationType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tlsconfig.dto.ArgumentDTO;
import com.tlsconfig.exception.TLSException;
import com.tlsconfig.utils.Constants;
import com.tlsconfig.utils.ReturnCodes;

public class CLIArgValidator {

    private static final Logger LOGGER = LogManager.getLogger(CLIArgValidator.class.getName());
    private static final String ARGUMENTS_ERROR = "Invalid arguments sent to TLSManagement.sh";
    private Options cliOptionsAvailable = new Options();
    private OptionGroup optionsGroup = new OptionGroup();

    public CLIArgValidator() throws TLSException {

        ResourceBundle optionDescription = null;
        try {
            optionDescription = new PropertyResourceBundle(
                    new FileInputStream(System.getProperty("com.tls.bundle.location")));
        } catch (IOException e) {
            throw new TLSException("Unable to resource bundle", e, ReturnCodes.FILE_NOT_FOUND.getValue());
        }

        final Option enable = Option.builder(Constants.SHORTENABLE).longOpt(Constants.ENABLE).hasArg()
                .desc(optionDescription.getString("enable")).build();
        final Option disable = Option.builder(Constants.SHORTDISABLE).longOpt(Constants.DISABLE).hasArg()
                .desc(optionDescription.getString("disable")).build();
        final Option noPrompt = Option.builder(Constants.SHORTNOPROMPT).longOpt(Constants.NOPROMPT)
                .hasArg(false).desc(optionDescription.getString("noPrompt")).build();
        final Option clearState = Option.builder(Constants.SHORTCLEARFAILEDSTATE)
                .longOpt(Constants.CLEARFAILEDSTATE).hasArg(false)
                .desc(optionDescription.getString("clearFailedState")).build();
        final Option state = Option.builder(Constants.SHORTSTATE).longOpt(Constants.STATE).hasArg(false)
                .desc(optionDescription.getString("stateDesc")).build();
        final Option ignoreState = Option.builder(Constants.SHORTIGNORECURRENTSTATE)
                .longOpt(Constants.IGNORECURRENTSTATE).hasArg(false)
                .desc(optionDescription.getString("ignoreCurrentState")).build();
        final Option help = Option.builder(Constants.SHORTHELP).longOpt(Constants.HELP).hasArg(false)
                .desc(optionDescription.getString("help")).build();

        cliOptionsAvailable.addOption(enable);
        cliOptionsAvailable.addOption(disable);
        cliOptionsAvailable.addOption(noPrompt);
        cliOptionsAvailable.addOption(ignoreState);
        cliOptionsAvailable.addOption(clearState);
        cliOptionsAvailable.addOption(state);
        cliOptionsAvailable.addOption(help);
   

        optionsGroup.setRequired(true);

        optionsGroup.addOption(enable);
        optionsGroup.addOption(disable);
        optionsGroup.addOption(clearState);
        optionsGroup.addOption(state);
        optionsGroup.addOption(help);

        cliOptionsAvailable.addOptionGroup(optionsGroup);
    }

    public ArgumentDTO handleScriptArguments(String[] cliArguments) throws TLSException {
        LOGGER.traceEntry("Starting to validate TLSManagement.sh arguments");
        LOGGER.info("Validation of TLSManagement.sh arguments started");
        ArgumentDTO adto = new ArgumentDTO();
        CommandLine commandLineInput = null;
        commandLineInput = getCommandLineParser(cliArguments);
        checkArgumentCount(commandLineInput);
        if (commandLineInput.hasOption(Constants.SHORTHELP)) {
            if (commandLineInput.hasOption(Constants.SHORTNOPROMPT)
                    || commandLineInput.hasOption(Constants.SHORTCLEARFAILEDSTATE)) {
                invalidArgumentsError();
            }
            adto.setOperationType(OperationType.help);
        } else if (commandLineInput.hasOption(Constants.SHORTCLEARFAILEDSTATE)) {
            if (commandLineInput.hasOption(Constants.SHORTNOPROMPT)
                    || commandLineInput.hasOption(Constants.SHORTHELP)) {
                invalidArgumentsError();
            }
            adto.setOperationType(OperationType.clearFailedState);
        } else if (commandLineInput.hasOption(Constants.SHORTSTATE)) {
            if (commandLineInput.hasOption(Constants.SHORTNOPROMPT)
                    || commandLineInput.hasOption(Constants.SHORTHELP)) {
                invalidArgumentsError();
            }
            adto.setOperationType(OperationType.systemState);
        } else if (commandLineInput.hasOption(Constants.SHORTDISABLE)) {
            adto.setOperationType(OperationType.disable);
            validateArgumentForEnableDisable(commandLineInput, adto, Constants.SHORTDISABLE);
        } else if (commandLineInput.hasOption(Constants.SHORTENABLE)) {
            adto.setOperationType(OperationType.enable);
            validateArgumentForEnableDisable(commandLineInput, adto, Constants.SHORTENABLE);
        }
        LOGGER.info("Validation of TLSManagement.sh arguments completed");
        LOGGER.traceExit("All TLSManagement.sh arguments validated successfully ");
        return adto;

    }

    private void invalidArgumentsError() throws TLSException {
        LOGGER.error(ARGUMENTS_ERROR);
        throw new TLSException(ARGUMENTS_ERROR, ReturnCodes.INVALIDARGUMENTSFORSCRIPT.getValue());
    }

    private void validateArgumentForEnableDisable(CommandLine commandLineInput, ArgumentDTO adto,
            String scriptArgument) throws TLSException {
        LOGGER.info("Validating arguments sent after {}", adto.getOperationType());
        if (Constants.TLSV1VERSION.equals(commandLineInput.getOptionValue(scriptArgument))) {
            adto.setVersion(Constants.TLSV1VERSION);
            if (commandLineInput.hasOption("n")) {
                LOGGER.info("Running TLSManagement.sh without prompt for {}", adto.getOperationType());
            } else {
                adto.setPromptRequired(true);
                LOGGER.info("Running TLSManagement.sh with prompt for {}", adto.getOperationType());
            }

            if(commandLineInput.hasOption(Constants.SHORTIGNORECURRENTSTATE)){
                adto.setIgnoreCurrentState(true);
                LOGGER.info("Running TLSManagement.sh without optimization for {}", adto.getOperationType());
            }

        } else {
            LOGGER.error("Invalid option sent to TLSManagement.sh after enable/disable");
            throw new TLSException("Invalid option sent to TLSManagement.sh after enable/disable",
                    ReturnCodes.INVALIDARGUMENTAFTERENABLEDISABLE.getValue());
        }
        LOGGER.info("Validation of arguments after {} done successfully ", adto.getOperationType());
    }

    private void checkArgumentCount(CommandLine commandLineInput) throws TLSException {
        if (!commandLineInput.getArgList().isEmpty()) {
            invalidArgumentsError();
        }
    }

    private CommandLine getCommandLineParser(String[] cliArguments) throws TLSException {
        final CommandLineParser inputParser = new DefaultParser();
        try {
            return inputParser.parse(cliOptionsAvailable, cliArguments);
        } catch (ParseException e) {
            LOGGER.error(ARGUMENTS_ERROR + e.getMessage(), e);
            throw new TLSException(ARGUMENTS_ERROR, ReturnCodes.INVALIDARGUMENTSFORSCRIPT.getValue());
        }
    }

    public void showHelpInConsole() {
        LOGGER.traceEntry("About to print help to the console");
        String lineSeparator = System.lineSeparator();
        StringBuilder helpContent = new StringBuilder();
        helpContent.append(lineSeparator).append("TLSConfigManagement.sh --enable TLSv1 [--noPrompt] [--ignoreCurrentState]")
                .append(lineSeparator).append("TLSConfigManagement.sh -e TLSv1 [-n] [-i]").append(lineSeparator)
                .append("TLSConfigManagement.sh --disable TLSv1 [--noPrompt] [--ignoreCurrentState]").append(lineSeparator)
                .append("TLSConfigManagement.sh -d TLSv1 [-n] [-i]").append(lineSeparator)
                .append("TLSConfigManagement.sh --clearFailedState").append(lineSeparator)
                .append("TLSConfigManagement.sh -c").append(lineSeparator)
                .append("TLSConfigManagement.sh --systemState").append(lineSeparator)
                .append("TLSConfigManagement.sh -s").append(lineSeparator)
                .append("TLSConfigManagement.sh --help").append(lineSeparator)
                .append("TLSConfigManagement.sh -h");
        String scriptHeader = "\nDescription: \nTool to manage TLSv1 protocol configurations of NetAct services centrally.\n\nArguments: ";
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.printHelp(helpContent.toString(), scriptHeader, cliOptionsAvailable, null);
        LOGGER.traceExit("Script with all the console options printed");
    }
}
