package org.esa.beam.meris.case2;

import org.esa.beam.framework.gpf.OperatorException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * TODO - Util class to read neural nets.
 *
 * @author Marco
 * @since Case2r 1.6
 */
public class NeuralNetReader {

    private NeuralNetReader() {
    }

    public static String readNeuralNetString(String resourceNetName, File neuralNetFile) {
        InputStream neuralNetStream;
        if (neuralNetFile == null) {
            neuralNetStream = MerisCase2BasisWaterOp.class.getResourceAsStream(resourceNetName);
        } else {
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                neuralNetStream = new FileInputStream(neuralNetFile);
            } catch (FileNotFoundException e) {
                throw new OperatorException(e);
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(neuralNetStream));
        try {
            String line = reader.readLine();
            final StringBuilder sb = new StringBuilder();
            while (line != null) {
                // have to append line terminator, cause it's not included in line
                sb.append(line).append('\n');
                line = reader.readLine();
            }
            return sb.toString();
        } catch (IOException ioe) {
            throw new OperatorException("Could not initialize neural net", ioe);
        } finally {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
    }
}
