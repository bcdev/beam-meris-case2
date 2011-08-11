package org.esa.beam.meris.case2.ui;

import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.meris.case2.Case2IOPOperator;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class Case2LakesAction extends AbstractVisatAction {

    private static final String OPERATOR_NAME = "Meris.Lakes";
    public static final String TARGET_NAME_SUFFIX = "_lakes";

    @Override
    public void actionPerformed(CommandEvent event) {
        final OperatorMetadata opMetadata = Case2IOPOperator.class.getAnnotation(OperatorMetadata.class);
        final String version = opMetadata.version();
        final Case2IOPDialog operatorDialog = new Case2IOPDialog(OPERATOR_NAME, TARGET_NAME_SUFFIX,
                                                                 getAppContext(), getText() + " - v" + version,
                                                                 event.getCommand().getHelpId()
        );
        operatorDialog.getJDialog().pack();
        operatorDialog.show();
    }
}