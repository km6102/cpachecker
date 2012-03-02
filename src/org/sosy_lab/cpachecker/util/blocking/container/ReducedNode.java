/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.blocking.container;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class ReducedNode {
  private static int uniqueNodeIdSequence = 0;

  public enum NodeKind {
      GENERIC       ("Generic"),
      FUNCTIONENTRY ("FunctionEntry"),
      FUNCTIONEXIT  ("FunctionExit"),
      LOOPHEAD      ("LoopHead");

      private final String text;

      public String getText() {
        return this.text;
      }

      private NodeKind(String pText) {
        this.text = pText;
      }
  };

  private final CFANode wrappedNode;
  private final int uniqueNodeId;
  private int summarizations;
  private boolean isAbstractioNode;
  private int functionCallId;
  private String[] callstack;

  public String[] getCallstack() {
    return this.callstack;
  }

  public ReducedNode(CFANode pWrappedNode) {
    this(pWrappedNode, null);
  }

  public ReducedNode(CFANode pWrappedNode, String[] pCallstack) {
    this.callstack = pCallstack;
    this.wrappedNode = pWrappedNode;
    this.uniqueNodeId = ReducedNode.uniqueNodeIdSequence++;
    this.summarizations = 0;
    this.functionCallId = 0;
    this.isAbstractioNode = false;
  }

  public CFANode getWrapped() {
    return this.wrappedNode;
  }

  public int getUniqueNodeId() {
    return this.uniqueNodeId;
  }

  public int getSummarizations() {
    return this.summarizations;
  }

  public void incSummarizations(int pIncBy) {
    this.summarizations += pIncBy;
  }

  public void setIsAbstractionNode(boolean pIsAbstractionNode) {
    this.isAbstractioNode = pIsAbstractionNode;
  }

  public boolean getIsAbstractionNode() {
    return this.isAbstractioNode || this.getWrapped().isLoopStart();
  }

  public boolean isFunctionEntry() {
    return getWrapped() instanceof CFAFunctionDefinitionNode;
  }

  public boolean isFunctionExit() {
    return getWrapped() instanceof CFAFunctionExitNode;
  }

  public boolean isLoopHead() {
    return getWrapped().isLoopStart();
  }

  public NodeKind getNodeKind() {
    if (isLoopHead()) {
      return NodeKind.LOOPHEAD;
    } else if (isFunctionEntry()) {
      return NodeKind.FUNCTIONENTRY;
    } else if (isFunctionExit()) {
      return NodeKind.FUNCTIONEXIT;
    } else {
      return NodeKind.GENERIC;
    }
  }

  public String getNodeKindText() {
    return getNodeKind().getText();
  }

  public void setFunctionCallId(int pCallId) {
    this.functionCallId = pCallId;
  }

  public int getFunctionCallId() {
    return this.functionCallId;
  }
}
