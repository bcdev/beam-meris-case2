package org.esa.beam.meris.case2.ui;

import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.meris.case2.Case2IOPOperator;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class Case2IOPAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final OperatorMetadata opMetadata = Case2IOPOperator.class.getAnnotation(OperatorMetadata.class);
        final String version = opMetadata.version();
        final Case2IOPDialog operatorDialog = new Case2IOPDialog(
                getAppContext(),
                "Case-2 IOP Processor - v" + version, "merisCase2IOP");
        // todo
//        operatorDialog.setTargetProductNameSuffix("_C2IOP");
        operatorDialog.getJDialog().pack();
        operatorDialog.show();
    }
}