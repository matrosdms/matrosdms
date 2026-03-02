/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.command;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;

/**
 * Root command for the Matrosdms CLI.
 * Prints usage when called without a sub-command.
 */
@Component
@Command(
        name = "matros",
        description = "Matrosdms CLI — document management utilities",
        subcommands = {
                LoginCommand.class,
                FindDuplicateCommand.class,
                OfflineDecryptCommand.class,
                picocli.CommandLine.HelpCommand.class
        },
        mixinStandardHelpOptions = true)
public class MatrosCommand implements Runnable {

    @Override
    public void run() {
        // Print usage if called without a sub-command.
        // picocli handles this automatically via mixinStandardHelpOptions = true.
    }
}
