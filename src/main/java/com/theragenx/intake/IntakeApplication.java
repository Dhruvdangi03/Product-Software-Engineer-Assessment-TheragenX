package com.theragenx.intake;

import com.theragenx.intake.cli.PipelineCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * One application, two ways to start it:
 * <ul>
 *   <li>without arguments it serves the REST API for the review UI;</li>
 *   <li>with {@code --pipeline} it runs the extraction pipeline once and exits.</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IntakeApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(IntakeApplication.class);
        if (PipelineCommand.isRequested(args)) {
            application.setWebApplicationType(WebApplicationType.NONE);
        }
        application.run(args);
    }
}
