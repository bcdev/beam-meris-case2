package org.esa.beam.meris.case2.ui;

import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.meris.case2.Case2IOPOperator;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class Case2RegionalAction extends AbstractVisatAction {

    private static final String OPERATOR_NAME = "Meris.Case2Regional";
    public static final String TARGET_NAME_SUFFIX = "_C2IOP";

    @Override
    public void actionPerformed(CommandEvent event) {
        final OperatorMetadata opMetadata = Case2IOPOperator.class.getAnnotation(OperatorMetadata.class);
        final String version = opMetadata.version();
        String title = "MERIS Case-2 Regional Processor - v" + version;
        final Case2IOPDialog operatorDialog = new Case2IOPDialog(OPERATOR_NAME, TARGET_NAME_SUFFIX,
                                                                 getAppContext(), title, event.getCommand().getHelpId());
        operatorDialog.getJDialog().pack();
        operatorDialog.show();
    }
}