package org.esa.beam.meris.case2.ui;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;

import java.util.HashMap;

class Case2IOPDialog extends SingleTargetProductDialog {

    private Case2IOPForm form;
    private HashMap<String, Object> parameters;
    private String operatorName;

    Case2IOPDialog(String operatorName, String targetNameSuffix, AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);
        this.operatorName = operatorName;

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operatorName");
        }

        parameters = new HashMap<String, Object>();
        PropertyContainer propContainer = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer(
                operatorName, parameters);
        propContainer.setDefaultValues();
        form = new Case2IOPForm(appContext, operatorSpi, propContainer, getTargetProductSelector(), targetNameSuffix);

        final OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorClass(),
                                                                                       propContainer, parameters, null);
        OperatorMenu menuSupport = new OperatorMenu(this.getJDialog(),
                                                    operatorSpi.getOperatorClass(), parameterSupport, helpID);
        getJDialog().setJMenuBar(menuSupport.createDefaultMenu());
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Product sourceProduct = form.getSourceProduct();
        return GPF.createProduct(operatorName, parameters, sourceProduct);
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();

    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();

    }
}
