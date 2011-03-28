package org.esa.beam.meris.case2.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.DefaultIOParametersPanel;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.meris.case2.Case2AlgorithmEnum;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class Case2IOPForm extends JTabbedPane {

    private AppContext appContext;
    private OperatorSpi operatorSpi;
    private PropertyContainer propertyContainer;
    private TargetProductSelector targetProductSelector;
    private DefaultIOParametersPanel ioParamPanel;

    Case2IOPForm(AppContext appContext, OperatorSpi operatorSpi, PropertyContainer container,
                 TargetProductSelector targetProductSelector) {
        this.appContext = appContext;
        this.operatorSpi = operatorSpi;
        this.propertyContainer = container;
        this.targetProductSelector = targetProductSelector;
        ioParamPanel = createIOParamTab();
        addTab("I/O Parameters", ioParamPanel);
        addTab("Processing Parameters", createProcessingParamTab());
    }

    private JScrollPane createProcessingParamTab() {
        PropertyPane parametersPane = new PropertyPane(propertyContainer);
        final BindingContext bindingContext = parametersPane.getBindingContext();
        bindingContext.bindEnabledState("doSmileCorrection", true, "doAtmosphericCorrection", true);
        bindingContext.bindEnabledState("outputTosa", true, "doAtmosphericCorrection", true);
        bindingContext.bindEnabledState("outputPath", true, "doAtmosphericCorrection", true);
        bindingContext.bindEnabledState("outputTransmittance", true, "doAtmosphericCorrection", true);
        bindingContext.bindEnabledState("landExpression", true, "doAtmosphericCorrection", true);
        bindingContext.bindEnabledState("cloudIceExpression", true, "doAtmosphericCorrection", true);
        bindingContext.addPropertyChangeListener("doAtmosphericCorrection", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Boolean enabled = (Boolean) evt.getNewValue();
                if (enabled) {
                    final Property invalidProperty = propertyContainer.getProperty("invalidPixelExpression");
                    final Object defaultValue = invalidProperty.getDescriptor().getDefaultValue();
                    propertyContainer.setValue("invalidPixelExpression", defaultValue);
                } else {
                    propertyContainer.setValue("invalidPixelExpression", "");
                }
            }
        });
        bindingContext.addPropertyChangeListener("algorithm", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Case2AlgorithmEnum algo = (Case2AlgorithmEnum) evt.getNewValue();
                setConversionValues(algo, propertyContainer);
            }
        });
        setConversionValues((Case2AlgorithmEnum) propertyContainer.getValue("algorithm"), propertyContainer);

        final JPanel parametersPanel = parametersPane.createPanel();
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        return new JScrollPane(parametersPanel);
    }

    private void setConversionValues(Case2AlgorithmEnum algo, PropertySet propertySet) {
        propertySet.setValue("chlConversionExponent", algo.getDefaultChlExponent());
        propertySet.setValue("chlConversionFactor", algo.getDefaultChlFactor());
        propertySet.setValue("tsmConversionExponent", algo.getDefaultTsmExponent());
        propertySet.setValue("tsmConversionFactor", algo.getDefaultTsmFactor());
    }

    private DefaultIOParametersPanel createIOParamTab() {
        return new DefaultIOParametersPanel(appContext, operatorSpi, targetProductSelector);
    }

    public void prepareShow() {
        ioParamPanel.initSourceProductSelectors();
    }

    public void prepareHide() {
        ioParamPanel.releaseSourceProductSelectors();
    }
}
