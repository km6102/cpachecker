/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class WitnessExporterTest {

  private enum WitnessGenerationConfig {
    K_INDUCTION("kInduction"),

    PREDICATE_ANALYSIS("predicateAnalysis");

    private final String fileName;

    private WitnessGenerationConfig(String pConfigName) {
      fileName = String.format("witnessGeneration-%s.properties", pConfigName);
    }
  }

  private static final String SPECIFICATION_OPTION = "specification";

  private static final String TEST_DIR_PATH = "test/programs/witnessValidation/";

  @Test(timeout = 10000)
  public void minepump_spec1_product05_true() throws Exception {
    newWitnessTester("minepump_spec1_product05_true-unreach-call_false-termination.cil.c")
        .useGenerationConfig(WitnessGenerationConfig.K_INDUCTION)
        .performTest();
  }

  @Test(timeout = 10000)
  public void minepump_spec1_product33_false() throws Exception {
    newWitnessTester("minepump_spec1_product33_false-unreach-call_false-termination.cil.c")
        .performTest()
        .useGenerationConfig(WitnessGenerationConfig.K_INDUCTION)
        .performTest();
  }

  private static void performTest(
      String pFilename,
      String pSpecification,
      String pGenerationConfig,
      Map<String, String> pOverrideOptions)
      throws Exception {
    String fullPath = Paths.get(TEST_DIR_PATH, pFilename).toString();

    TempCompressedFilePath witnessPath = new TempCompressedFilePath("witness", ".graphml");

    WitnessType witnessType =
        generateWitness(fullPath, pGenerationConfig, pSpecification, pOverrideOptions, witnessPath);

    validateWitness(fullPath, pSpecification, pOverrideOptions, witnessPath, witnessType);
  }

  private static WitnessType generateWitness(
      String pFilePath,
      String pGenerationConfig,
      String pSpecification,
      Map<String, String> pOverrideOptions,
      TempCompressedFilePath pWitnessPath)
      throws Exception {
    Map<String, String> overrideOptions = Maps.newHashMap(pOverrideOptions);
    overrideOptions.put(
        "counterexample.export.graphml", pWitnessPath.uncompressedFilePath.toString());
    overrideOptions.put("cpa.arg.proofWitness", pWitnessPath.uncompressedFilePath.toString());
    overrideOptions.put("bmc.invariantsExport", pWitnessPath.uncompressedFilePath.toString());
    Configuration generationConfig =
        getProperties(pGenerationConfig, overrideOptions, pSpecification);

    TestResults results = CPATestRunner.run(generationConfig, pFilePath);
    // Trigger statistics so that the witness is written to the file
    results.getCheckerResult().printStatistics(new PrintStream(ByteStreams.nullOutputStream()));

    if (isSupposedToBeSafe(pFilePath)) {
      results.assertIsSafe();
      return WitnessType.CORRECTNESS_WITNESS;
    } else if (isSupposedToBeUnsafe(pFilePath)) {
      results.assertIsUnsafe();
      return WitnessType.VIOLATION_WITNESS;
    }
    Assert.fail("Cannot determine expected result.");
    throw new AssertionError("Unreachable code.");
  }

  private static void validateWitness(
      String pFilePath,
      String pSpecification,
      Map<String, String> pOverrideOptions,
      TempCompressedFilePath witnessPath,
      WitnessType witnessType)
      throws Exception {
    Map<String, String> overrideOptions;
    overrideOptions = Maps.newHashMap(pOverrideOptions);
    final String validationConfigFile;
    String specification = pSpecification;
    switch (witnessType) {
      case CORRECTNESS_WITNESS:
        validationConfigFile = "correctnessWitnessValidation.properties";
        overrideOptions.put(
            "invariantGeneration.kInduction.invariantsAutomatonFile",
            witnessPath.uncompressedFilePath.toString());
        break;
      case VIOLATION_WITNESS:
        validationConfigFile = "violationWitnessValidation.properties";
        specification =
            Joiner.on(',').join(specification, witnessPath.compressedFilePath.toString());
        break;
      default:
        throw new AssertionError("Unsupported witness type " + witnessType);
    }
    Configuration validationConfig =
        getProperties(validationConfigFile, overrideOptions, specification);

    TestResults results = CPATestRunner.run(validationConfig, pFilePath);

    if (isSupposedToBeSafe(pFilePath)) {
      results.assertIsSafe();
    } else if (isSupposedToBeUnsafe(pFilePath)) {
      results.assertIsUnsafe();
    } else {
      Assert.fail("Cannot determine expected result.");
    }
  }

  private static Configuration getProperties(
      String pConfigFile, Map<String, String> pOverrideOptions, String pSpecification)
      throws InvalidConfigurationException {
    ConfigurationBuilder configBuilder =
        TestDataTools.configurationForTest()
            .loadFromResource(WitnessExporterTest.class, pConfigFile);
    if (!Strings.isNullOrEmpty(pSpecification)) {
      pOverrideOptions.put(SPECIFICATION_OPTION, pSpecification);
    }
    return configBuilder.setOptions(pOverrideOptions).build();
  }

  private static boolean isSupposedToBeUnsafe(String pFilePath) {
    return pFilePath.contains("_false_assert") || pFilePath.contains("_false-unreach");
  }

  private static boolean isSupposedToBeSafe(String pFilePath) {
    return pFilePath.contains("_true_assert") || pFilePath.contains("_true-unreach");
  }

  private static class TempCompressedFilePath {

    private final Path uncompressedFilePath;

    private final Path compressedFilePath;

    public TempCompressedFilePath(String pPrefix, String pSuffix) throws IOException {
      String compressedSuffix = ".gz";
      compressedFilePath =
          TempFile.builder()
              .prefix(pPrefix)
              .suffix(pSuffix + compressedSuffix)
              .create()
              .toAbsolutePath();
      String violationWitnessFileName = compressedFilePath.getFileName().toString();
      String uncompressedWitnessFileName =
          violationWitnessFileName.substring(
              0, violationWitnessFileName.length() - compressedSuffix.length());
      uncompressedFilePath = compressedFilePath.resolveSibling(uncompressedWitnessFileName);
      uncompressedFilePath.toFile().deleteOnExit();
    }

    @Override
    public String toString() {
      return compressedFilePath.toString();
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof TempCompressedFilePath) {
        return compressedFilePath.equals(((TempCompressedFilePath) pOther).compressedFilePath);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return compressedFilePath.hashCode();
    }
  }

  private static class WitnessTester {

    private final String programFile;

    private WitnessGenerationConfig generationConfig = WitnessGenerationConfig.PREDICATE_ANALYSIS;

    private String specificationFile = "config/specification/default.spc";

    private ImmutableMap.Builder<String, String> overrideOptionsBuilder = ImmutableMap.builder();

    private WitnessTester(String pProgramFile) {
      programFile = Objects.requireNonNull(pProgramFile);
    }

    @CanIgnoreReturnValue
    public WitnessTester useGenerationConfig(WitnessGenerationConfig pGenerationConfig) {
      generationConfig = Objects.requireNonNull(pGenerationConfig);
      return this;
    }

    @CanIgnoreReturnValue
    public WitnessTester forSpecification(String pSpecificationFile) {
      specificationFile = Objects.requireNonNull(pSpecificationFile);
      return this;
    }

    @CanIgnoreReturnValue
    public WitnessTester addOverrideOption(String pOptionName, String pOptionValue) {
      overrideOptionsBuilder.put(pOptionName, pOptionValue);
      return this;
    }

    @CanIgnoreReturnValue
    public WitnessTester performTest() throws Exception {
      WitnessExporterTest.performTest(
          programFile,
          specificationFile,
          generationConfig.fileName,
          overrideOptionsBuilder.build());
      return this;
    }
  }

  public static WitnessTester newWitnessTester(String pProgramFile) {
    return new WitnessTester(pProgramFile);
  }
}
