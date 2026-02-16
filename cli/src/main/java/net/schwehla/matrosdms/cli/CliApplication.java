/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import net.schwehla.matrosdms.cli.command.OfflineDecryptCommand;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
public class CliApplication implements CommandLineRunner, ExitCodeGenerator {

    @Autowired
    private OfflineDecryptCommand decryptCommand;

    @Autowired
    private IFactory factory; // Spring factory for Picocli injection

    private int exitCode;

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(CliApplication.class, args)));
    }

    @Override
    public void run(String... args) {
        // If no args are passed, show help
        if (args.length == 0) {
            args = new String[]{"--help"};
        }
        
        exitCode = new CommandLine(decryptCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}