package se.kth.jabeja.config;

public class Config {
  private Integer numPartitions;
  private Integer rounds;
  private Integer randomNeighborsSampleSize;
  private Float temperature;
  private Float delta;
  private Integer seed;
  private Integer uniformRandomSampleSize;
  private String graphFile;
  private String outputDir;
  private GraphInitColorPolicy initColorPolicy;
  private NodeSelectionPolicy nodeSelectionPolicy;
  private Float alpha;
  private boolean restart;
  private int restartInterval;

  //-----------------Added by Denys Tykhoglo----------------------------
  private boolean enhanced;
  private float tempEnh;
  private float minTempEnh;
  private float alphaEnh;
  private int iterEnh;
  //-----------------Added by Denys Tykhoglo----------------------------

  public Config setAlpha(Float alpha) {
    this.alpha = alpha;
    return this;
  }

  public Config setGraphInitialColorPolicy(GraphInitColorPolicy policy) {
    this.initColorPolicy = policy;
    return this;
  }

  public Config setOutputDir(String outputDir) {
    this.outputDir = outputDir;
    return this;
  }

  public Config setNodeSelectionPolicy(NodeSelectionPolicy nodeSelectionPolicy) {
    this.nodeSelectionPolicy = nodeSelectionPolicy;
    return this;
  }

  public Config setGraphFilePath(String graphFilePath) {
    this.graphFile = graphFilePath;
    return this;
  }

  public Config setNumPartitions(Integer num_partitions) {
    this.numPartitions = num_partitions;
    return this;
  }

  public Config setRounds(Integer rounds) {
    this.rounds = rounds;
    return this;
  }

  public Config setRandNeighborsSampleSize(Integer closeby_neighbors) {
    this.randomNeighborsSampleSize = closeby_neighbors;
    return this;
  }

  public Config setTemperature(Float temperature) {
    this.temperature = temperature;
    return this;
  }

  public Config setDelta(Float delta) {
    this.delta = delta;
    return this;
  }

  public Config setSeed(Integer seed) {
    this.seed = seed;
    return this;
  }

  public Config setUniformRandSampleSize(Integer rnd_list_size) {
    this.uniformRandomSampleSize = rnd_list_size;
    return this;
  }

  public Config setRestart(boolean restart) {
    this.restart = restart;
    return this;
  }

  public Config setRestartInterval(int restartInterval) {
    this.restartInterval = restartInterval;
    return this;
  }

  //-----------------Added by Denys Tykhoglo----------------------------
  public Config setEnhanced(boolean enhanced) {
    this.enhanced = enhanced;
    return this;
  }

  public Config setTempEnh(float tempEnh) {
    this.tempEnh = tempEnh;
    return this;
  }

  public Config setMinTempEnh(float minTempEnh) {
    this.minTempEnh = minTempEnh;
    return this;
  }

  public Config setAlphaEnh(float alphaEnh) {
    this.alphaEnh = alphaEnh;
    return this;
  }

  public Config setIterEnh(int iterEnh) {
    this.iterEnh = iterEnh;
    return this;
  }
  //-----------------Added by Denys Tykhoglo----------------------------

  public Integer getNumPartitions() {
    if (numPartitions == null) {
      throw new NullPointerException("Num partitions is not set");
    }
    return numPartitions;
  }

  public Integer getRounds() {
    if (rounds == null) {
      throw new NullPointerException("Rounds is not set");
    }
    return rounds;
  }

  public Integer getRandomNeighborSampleSize() {
    if (randomNeighborsSampleSize == null) {
      throw new NullPointerException("Close by neighbors are not set");
    }
    return randomNeighborsSampleSize;
  }

  public Float getTemperature() {
    if (temperature == null) {
      throw new NullPointerException("Temperature is not set");
    }
    return temperature;
  }

  public Float getDelta() {
    if (delta == null) {
      throw new NullPointerException("Delta is not set");
    }
    return delta;
  }

  public Integer getSeed() {
    if (seed == null) {
      throw new NullPointerException("Seed is not set");
    }
    return seed;
  }

  public Integer getUniformRandomSampleSize() {
    if (uniformRandomSampleSize == null) {
      throw new NullPointerException("Random list size is not set");
    }
    return uniformRandomSampleSize;
  }

  public String getGraphFilePath() {
    if (graphFile == null) {
      throw new NullPointerException("Graph file path is not set");
    }
    return graphFile;
  }

  public GraphInitColorPolicy getGraphInitialColorPolicy() {
    if (initColorPolicy == null) {
      throw new NullPointerException("Graph initial color policy is not defined.");
    }
    return initColorPolicy;
  }

  public NodeSelectionPolicy getNodeSelectionPolicy() {
    if (nodeSelectionPolicy == null) {
      throw new NullPointerException("Node selection policy is not defined.");
    }
    return nodeSelectionPolicy;
  }

  public String getOutputDir() {
    if (outputDir == null) {
      throw new NullPointerException("Output dir is not set");
    }
    return outputDir;

  }

  public Float getAlpha() {
    if (alpha == null) {
    }
    return alpha;
  }

  //-----------------Added by Denys Tykhoglo----------------------------
  public boolean getEnhanced() {
    return enhanced;
  }

  public float getTempEnh() {
    return tempEnh;
  }

  public float getMinTempEnh() {
    return minTempEnh;
  }

  public float getAlphaEnh() {
    return alphaEnh;
  }

  public int getIterEnh() {
    return iterEnh;
  }

  public boolean getRestart() {
    return restart;
  }

  public int getRestartInterval() {
    return restartInterval;
  }
  //-----------------Added by Denys Tykhoglo----------------------------

  public Config createJabejaConfig() {
    return new Config();
  }

}