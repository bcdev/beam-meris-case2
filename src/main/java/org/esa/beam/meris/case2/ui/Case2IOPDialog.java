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

    private static final String OPERATOR_NAME = "Meris.Case2IOP";
    private Case2IOPForm form;
    private HashMap<String, Object> parameters;

    Case2IOPDialog(AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(OPERATOR_NAME);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operatorName");
        }

        parameters = new HashMap<String, Object>();
        PropertyContainer propContainer = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer(
                OPERATOR_NAME, parameters);
        propContainer.setDefaultValues();
        form = new Case2IOPForm(appContext, operatorSpi, propContainer, getTargetProductSelector());


        final OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorClass(),
                                                                                       propContainer, parameters, null);
        OperatorMenu menuSupport = new OperatorMenu(this.getJDialog(),
                                                    operatorSpi.getOperatorClass(), parameterSupport, helpID);
        getJDialog().setJMenuBar(menuSupport.createDefaultMenu());
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Product sourceProduct = form.getSourceProduct();
        return GPF.createProduct(OPERATOR_NAME, parameters, sourceProduct);
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
