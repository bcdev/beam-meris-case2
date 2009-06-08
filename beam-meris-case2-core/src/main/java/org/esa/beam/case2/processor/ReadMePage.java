package org.esa.beam.case2.processor;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.ui.ParameterPage;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ReadMePage extends ParameterPage {

    private URL urlToFile;

    /**
     * Creates a new instance of this class.
     *
     * @param urlToFile
     */
    public ReadMePage(URL urlToFile) {
        super(new ParamGroup());
        setTitle("Read Me");
        this.urlToFile = urlToFile;
    }

    public JComponent createUI() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableWeightX(1.0);
        layout.setRowWeightY(0, 1.0);
        final JPanel mainPanel = new JPanel(layout);
        mainPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        mainPanel.add(createReadmePanel(urlToFile));

        return mainPanel;
    }


    @Override
    public void initRequestFromUI(Request request) throws ProcessorException {
        // ignore
    }

    @Override
    public void setUIFromRequest(Request request) throws ProcessorException {
        // ignore
    }

    private static JPanel createReadmePanel(URL readMeUrl) {
        final TableLayout layout = new TableLayout(2);
        layout.setColumnWeightX(0, 1.0);
        layout.setColumnWeightY(0, 1.0);
        layout.setCellFill(0, 0, TableLayout.Fill.BOTH);
        final JPanel informationPane = new JPanel(layout);

        final HyperlinkListener hyperlinkListener = new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        } catch (URISyntaxException e1) {
                            e1.printStackTrace();
                        }

                    }

                }
            }
        };
        try {
            final JEditorPane textPane = new JEditorPane();
            textPane.setPage(readMeUrl);
            textPane.setEditable(false);
            textPane.addHyperlinkListener(hyperlinkListener);
            final JScrollPane scrollPane = new JScrollPane(textPane);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setBorder(new TitledBorder("Read Me"));
            informationPane.add(scrollPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return informationPane;
    }

}