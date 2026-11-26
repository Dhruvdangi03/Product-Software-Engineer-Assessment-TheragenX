package com.theragenx.intake.cli;

import com.theragenx.intake.extraction.ExtractionPipeline;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Runs the extraction pipeline when the application is started with {@code --pipeline}.
 * Every setting is an ordinary property, so it can be overridden on the command line, e.g.
 * <pre>
 *   java -jar target/case-intake.jar --pipeline --intake.llm.mode=replay --intake.catalog-path=my_catalog.json
 * </pre>
 */
@Component
public class PipelineCommand implements ApplicationRunner {

    static final String PIPELINE_OPTION = "pipeline";

    private final ExtractionPipeline pipeline;

    public PipelineCommand(ExtractionPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public static boolean isRequested(String[] args) {
        return Arrays.asList(args).contains("--" + PIPELINE_OPTION);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption(PIPELINE_OPTION)) {
            pipeline.run();
        }
    }
}
