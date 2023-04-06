package combinatorial_molecule_library_design;


public class BinEntropyCalculator extends EntropyCalculator{

    private double blowupFactor;
    private double[] lowerBounds;
    private double[] upperBounds;
    private int[] distribution;

    public BinEntropyCalculator(double[][] bbMasses, double blowupFactor, double binSize){
        super(bbMasses);
        this.blowupFactor = blowupFactor;

        // Compute the lower and upper bounds of each sub-interval in [minMoleculeMass, maxMoleculeMass].
        double minMoleculeMass = this.getMinMoleculeMass();
        double maxMoleculeMass = this.getMaxMoleculeMass();
        int numBins = (int) Math.ceil((maxMoleculeMass - minMoleculeMass) / binSize);
        double adjustedBinSize = (maxMoleculeMass - minMoleculeMass) / numBins;

        double[] lowerBounds = new double[numBins];
        double[] upperBounds = new double[numBins];

        lowerBounds[0] = minMoleculeMass;
        upperBounds[numBins-1] = maxMoleculeMass;
        for(int i = 1; i < numBins; i++){
            lowerBounds[i] = minMoleculeMass + i * adjustedBinSize;
            upperBounds[i-1] = lowerBounds[i] - 1 / this.blowupFactor;
        }
    }

    private double getMinMoleculeMass(){
        // It is assumed that the bbMasses[i] are sorted from min to max:
        double minMass = 0;
        for(int i = 0; i < this.bbMasses.length; i++){
            minMass = minMass + this.bbMasses[i][0];
        }
        return minMass;
    }

    private double getMaxMoleculeMass(){
        // It is assumed that the bbMasses[i] are sorted from min to max:
        double maxMass = 0;
        for(int i = 0; i < this.bbMasses.length; i++){
            int idxMaxBBMass = this.bbMasses[i].length - 1;
            maxMass = maxMass + this.bbMasses[i][idxMaxBBMass];
        }
        return maxMass;
    }

    @Override
    public double computeEntropy() {
        // For each bin, compute the number of molecules those mass is contained in the bin:
        MassDecomposer decomposer = new MassDecomposer(this.bbMasses, this.blowupFactor);
        this.distribution = new int[this.getNumberOfBins()];
        for(int i = 0; i < this.distribution.length; i++){
            this.distribution[i] = decomposer.numberOfMoleculesForInterval(this.lowerBounds[i], this.upperBounds[i]);
        }

        // Convert the absolute frequencies into relative frequencies:
        int totalNumMolecules = 0;
        for (int j : this.distribution) totalNumMolecules += j;
        double[] relativeFrequencies = new double[this.distribution.length];
        for(int i = 0; i < this.distribution.length; i++){
            relativeFrequencies[i] = ((double) this.distribution[i]) / totalNumMolecules;
        }

        // Compute the entropy:
        this.entropy = 0;
        for(double p : relativeFrequencies){
            if(p > 0){
                this.entropy = this.entropy + p * Math.log(p);
            }
        }
        this.entropy = -1 * this.entropy;
        return this.entropy;
    }

    public double getEntropy(){
        return this.entropy;
    }

    public double[] getLowerBounds(){
        return this.lowerBounds;
    }

    public double[] getUpperBounds(){
        return this.upperBounds;
    }

    public int[] getNumMoleculesPerBin(){
        return this.distribution;
    }

    public double getBlowupFactor(){
        return this.blowupFactor;
    }

    public int getNumberOfBins(){
        return this.lowerBounds.length;
    }
}
