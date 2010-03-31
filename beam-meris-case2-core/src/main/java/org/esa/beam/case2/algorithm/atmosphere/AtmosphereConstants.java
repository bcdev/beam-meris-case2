package org.esa.beam.case2.algorithm.atmosphere;

public interface AtmosphereConstants {

    // @todo - read from auxdata for later versions
    static final double[] merband12 = {
            412.3, 442.3, 489.7,
            509.6, 559.5, 619.4,
            664.3, 680.6, 708.1,
            753.1, 778.2, 864.6};
    static final double[] merband9 = {
            412.3, 442.3, 489.7,
            509.6, 559.5, 619.4,
            664.3, 680.6, 708.1};
    static final int[] merband12_index = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12};

    // absorption by ozone for MERIS bands 1-10, 12, 13
    // todo - are only used as negatives, make them naegative here
    static final double[] absorb_ozon = {
            8.2e-004, 2.82e-003, 2.076e-002, 3.96e-002, 1.022e-001,
            1.059e-001, 5.313e-002, 3.552e-002, 1.895e-002, 8.38e-003,
            7.2e-004, 0.0};


}
