/*
 * BNEZ.java
 *
 * may 2006
 * Instruction BNEZ of the MIPS64 Instruction Set
 * (c) 2006 EduMips64 project - Trubia Massimo, Russo Daniele
 *
 * This file is part of the EduMIPS64 project, and is released under the GNU
 * General Public License.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.edumips64.core.is;

import org.edumips64.core.*;
import org.edumips64.utils.*;
/** <pre>
 *  Syntax:        BNEZ rs, offset
 *  Description:   To test a GPR then do a PC-relative conditional branch
 *</pre>
 * @author Trubia Massimo, Russo Daniele
 */
public class BNEZ extends FlowControl_IType {
  public String OPCODE_VALUE = "000111";
  protected final int OFFSET_FIELD = 1;
  private String PREDICTION_SCHEME = "2-Bit Saturating";
  private boolean PREDICTION = false;
  private String IF_PC_VALUE = "";
  private int PREDICTOR_ADDRESS = 0;

  /** Creates a new instance of BEQZ */
  public BNEZ() {
    super.OPCODE_VALUE = OPCODE_VALUE;
    syntax = "%R,%B";
    name = "BNEZ";
  }

  public void IF() throws IrregularStringOfBitsException, IrregularWriteOperationException, TwosComplementSumException {
    Dinero din = Dinero.getInstance();

    try {
      din.IF(Converter.binToHex(Converter.intToBin(64, cpu.getLastPC().getValue())));
    } catch (IrregularStringOfBitsException e) {
      e.printStackTrace();
    }

    Register pc = cpu.getPC();
    IF_PC_VALUE = pc.getBinString();

    if(PREDICTION_SCHEME == "Always True") {
      PREDICTION = true;
    } else if(PREDICTION_SCHEME == "2-Bit Saturating") { // need 12 least sig bits of PC
      PREDICTOR_ADDRESS = 0;
      for(int i = 0; i < 12; i++) {
        if(IF_PC_VALUE.charAt(i) == '1')
          PREDICTOR_ADDRESS += Math.pow(2, i);
      }
      PREDICTION = cpu.getSaturatingBranchPrediction(PREDICTOR_ADDRESS);
    } else { PREDICTION = false; }

    if(PREDICTION == true) {
      //TESTING PREDICT TRUE BRANCHING
      //converting offset into a signed binary value of 64 bits in length
      BitSet64 bs = new BitSet64();
      bs.writeHalf(params.get(OFFSET_FIELD));
      String offset = bs.getBinString();
    
      String pc_new = "";

      //updating program counter
      pc_new = InstructionsUtils.twosComplementSum(IF_PC_VALUE, offset);
      pc.setBits(pc_new, 0);
    }
  }

  public void ID() throws RAWException, IrregularWriteOperationException, IrregularStringOfBitsException, JumpException, TwosComplementSumException {
    //getting registers rs and rt
    if (cpu.getRegister(params.get(RS_FIELD)).getWriteSemaphore() > 0) {
      throw new RAWException();
    }
  }

  public void EX()
  throws IrregularStringOfBitsException, IntegerOverflowException, IrregularWriteOperationException, JumpException, TwosComplementSumException, BranchMispredictionException {
    //getting registers rs and rt
    //if (cpu.getRegister(params.get(RS_FIELD)).getWriteSemaphore() > 0) {
      //throw new RAWException();
    //}

    String rs = cpu.getRegister(params.get(RS_FIELD)).getBinString();
    String zero = Converter.positiveIntToBin(64, 0);
    //converting offset into a signed binary value of 64 bits in length
    BitSet64 bs = new BitSet64();
    bs.writeHalf(params.get(OFFSET_FIELD));
    String offset = bs.getBinString();
    boolean condition = ! rs.equals(zero);

    if(PREDICTION_SCHEME == "2-Bit Saturating")
      cpu.updateSaturatingBranchPredictor(PREDICTOR_ADDRESS, condition);

    if(PREDICTION == true) { // always predict true
    if (!condition) {
      Register pc = cpu.getPC();

      //updating program counter to branch fallthrough
      pc.setBits(IF_PC_VALUE, 0);

      throw new BranchMispredictionException();
    }
    } else if(PREDICTION == false) { // always predict false
    if (condition) {
      Register pc = cpu.getPC();
      String pc_new = "";

      //updating program counter to branch location
      
      pc_new = InstructionsUtils.twosComplementSum(IF_PC_VALUE, offset);
      pc.setBits(pc_new, 0);
      throw new BranchMispredictionException();
    }
    }
  }
  public void pack() throws IrregularStringOfBitsException {

    repr.setBits(OPCODE_VALUE, OPCODE_VALUE_INIT);
    repr.setBits(Converter.intToBin(RS_FIELD_LENGTH, 0/*params.get(RS_FIELD)*/), RS_FIELD_INIT);
    repr.setBits(Converter.intToBin(RT_FIELD_LENGTH, params.get(RS_FIELD) /*0*/), RT_FIELD_INIT);
    repr.setBits(Converter.intToBin(OFFSET_FIELD_LENGTH, params.get(OFFSET_FIELD) / 4), OFFSET_FIELD_INIT);
  }
}
