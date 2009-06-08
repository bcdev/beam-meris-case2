package org.esa.beam.lakes.eutrophic.processor;


import org.esa.beam.framework.processor.ProcessorRunner;

/**
 * This class is the entry point for the MERIS Eutrophic Lakes Processor when invoked from
 * the command line. The command line arguments that can be understood by the processor are:
 * <ul>
 * <li>-i or --interactive (optional): open the user interface - in this case just a default interface
 * stating that this processor has no user interface implementation</li>
 * <li>-d or --debug (optional): swicth the BEAM framework into debugging mode. This will give a wealth of
 * additional state information logged to the console window</li>
 * <li>the path to a request file (mandatory)</li>
 * </ul>
 */
public class EutrophicLakesProcessorMain {

    /**
     * Runs this module as stand-alone application
     *
     * @param args the command-line arguments
     */
    public static void main(final String[] args) {
        ProcessorRunner.runProcessor(args, new EutrophicLakesProcessor());
    }
}
