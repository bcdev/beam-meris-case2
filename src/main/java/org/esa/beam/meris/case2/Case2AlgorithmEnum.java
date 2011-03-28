package org.esa.beam.meris.case2;

public enum Case2AlgorithmEnum {

    REGIONAL {
        @Override
        public MerisCase2BasisWaterOp createOperatorInstance() {
            return new RegionalWaterOp();
        }

        @Override
        public double getDefaultTsmExponent() {
            return 1.0;
        }

        @Override
        public double getDefaultTsmFactor() {
            return 1.73;
        }

        @Override
        public double getDefaultChlExponent() {
            return 1.04;
        }

        @Override
        public double getDefaultChlFactor() {
            return 21.0;
        }
    },
    BOREAL {
        @Override
        public MerisCase2BasisWaterOp createOperatorInstance() {
            return new BorealWaterOp();
        }

        @Override
        public double getDefaultTsmExponent() {
            return Double.NaN;
        }

        @Override
        public double getDefaultTsmFactor() {
            return Double.NaN;
        }

        @Override
        public double getDefaultChlExponent() {
            return Double.NaN;
        }

        @Override
        public double getDefaultChlFactor() {
            return Double.NaN;
        }
    },
    EUTROPHIC {
        @Override
        public MerisCase2BasisWaterOp createOperatorInstance() {
            return new EutrophicWaterOp();
        }

        @Override
        public double getDefaultTsmExponent() {
            return 1.0;
        }

        @Override
        public double getDefaultTsmFactor() {
            return 1.73;
        }

        @Override
        public double getDefaultChlExponent() {
            return 1.0;
        }

        @Override
        public double getDefaultChlFactor() {
            return 0.0318;
        }
    };

    public abstract MerisCase2BasisWaterOp createOperatorInstance();

    public abstract double getDefaultTsmExponent();

    public abstract double getDefaultTsmFactor();

    public abstract double getDefaultChlExponent();

    public abstract double getDefaultChlFactor();
}
