package net.sf.emustudio.zilogZ80.impl;

import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.zilogZ80.impl.suite.ByteTestBuilder;
import net.sf.emustudio.zilogZ80.impl.suite.FlagsBuilderImpl;
import net.sf.emustudio.zilogZ80.impl.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_L;
import static net.sf.emustudio.zilogZ80.impl.suite.Utils.get8MSBplus8LSB;
import static net.sf.emustudio.zilogZ80.impl.suite.Utils.predicate8MSBplus8LSB;

@SuppressWarnings("unchecked")
public class ArithmeticTest extends InstructionsTTest {

    private ByteTestBuilder additionTestBuilder() {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();
    }

    private ByteTestBuilder subtractionTestBuilder() {
        return new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
                .firstIsRegister(REG_A)
                .keepCurrentInjectorsAfterRun();
    }

    @Test
    public void testADD_A__r() throws Exception {
        ByteTestBuilder test = additionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x87)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x80),
                test.secondIsRegister(REG_C).run(0x81),
                test.secondIsRegister(REG_D).run(0x82),
                test.secondIsRegister(REG_E).run(0x83),
                test.secondIsRegister(REG_H).run(0x84),
                test.secondIsRegister(REG_L).run(0x85),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x86)
        );
    }

    @Test
    public void testADD_A__n() throws Exception {
        ByteTestBuilder test = additionTestBuilder();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xC6)
        );
    }

    @Test
    public void testADC_A__r() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & FLAG_C))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
                .firstIsRegister(REG_A)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x8F)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x88),
                test.secondIsRegister(REG_C).run(0x89),
                test.secondIsRegister(REG_D).run(0x8A),
                test.secondIsRegister(REG_E).run(0x8B),
                test.secondIsRegister(REG_H).run(0x8C),
                test.secondIsRegister(REG_L).run(0x8D),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x8E)
        );
    }

    @Test
    public void testADC_A__n() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & FLAG_C))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
                .firstIsRegister(REG_A);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xCE)
        );
    }

    @Test
    public void testSUB_A__r() throws Exception {
        ByteTestBuilder test = subtractionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x97)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x90),
                test.secondIsRegister(REG_C).run(0x91),
                test.secondIsRegister(REG_D).run(0x92),
                test.secondIsRegister(REG_E).run(0x93),
                test.secondIsRegister(REG_H).run(0x94),
                test.secondIsRegister(REG_L).run(0x95),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x96)
        );
    }

    @Test
    public void testSUB_A__n() throws Exception {
        ByteTestBuilder test = subtractionTestBuilder();

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xD6)
        );
    }

    @Test
    public void testSBC_A__r() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & FLAG_C))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
                .firstIsRegister(REG_A)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x9F)
        );
        Generator.forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x98),
                test.secondIsRegister(REG_C).run(0x99),
                test.secondIsRegister(REG_D).run(0x9A),
                test.secondIsRegister(REG_E).run(0x9B),
                test.secondIsRegister(REG_H).run(0x9C),
                test.secondIsRegister(REG_L).run(0x9D),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x9E)
        );
    }

    @Test
    public void testSBC_A__n() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & FLAG_C))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
                .firstIsRegister(REG_A);

        Generator.forSome8bitBinary(
                test.runWithSecondOperand(0xDE)
        );
    }

    @Test
    public void testINC__r() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyFlags(new FlagsBuilderImpl().sign().zero().overflow().halfCarry().subtractionIsReset(), context -> context.first + 1)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.verifyRegister(REG_B).firstIsRegister(REG_B).run(0x04)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_C).firstIsRegister(REG_C).run(0x0C)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_D).firstIsRegister(REG_D).run(0x14)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_E).firstIsRegister(REG_E).run(0x1C)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_H).firstIsRegister(REG_H).run(0x24)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_L).firstIsRegister(REG_L).run(0x2C)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_A).firstIsRegister(REG_A).run(0x3C)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyByte(1).setPair(REG_PAIR_HL, 1).firstIsMemoryByteAt(1).run(0x34)
        );
    }

    @Test
    public void testDEC__r() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyFlags(new FlagsBuilderImpl().sign().zero().overflow().halfCarry().subtractionIsSet(), context -> context.first - 1)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.verifyRegister(REG_B).firstIsRegister(REG_B).run(0x05)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_C).firstIsRegister(REG_C).run(0x0D)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_D).firstIsRegister(REG_D).run(0x15)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_E).firstIsRegister(REG_E).run(0x1D)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_H).firstIsRegister(REG_H).run(0x25)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_L).firstIsRegister(REG_L).run(0x2D)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_A).firstIsRegister(REG_A).run(0x3D)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyByte(1).setPair(REG_PAIR_HL, 1).firstIsMemoryByteAt(1).run(0x35)
        );
    }

    @Test
    public void testINC__ss() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first + 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).firstIsPair(REG_PAIR_BC).run(0x03)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_DE, verifier).firstIsPair(REG_PAIR_DE).run(0x13)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_HL, verifier).firstIsPair(REG_PAIR_HL).run(0x23)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_SP, verifier).firstIsPair(REG_SP).run(0x33)
        );
    }

    @Test
    public void testDEC__ss() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl);
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first - 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).firstIsPair(REG_PAIR_BC).run(0x0B)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_DE, verifier).firstIsPair(REG_PAIR_DE).run(0x1B)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_HL, verifier).firstIsPair(REG_PAIR_HL).run(0x2B)
        );

        test.clearVerifiers();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_SP, verifier).firstIsPair(REG_SP).run(0x3B)
        );
    }

    @Test
    public void testADD_HL__ss() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyPair(REG_PAIR_HL, context -> context.first + context.second)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().carry15().halfCarry11().subtractionIsReset())
                .firstIsPair(REG_PAIR_HL)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
                test.run(0x29)
        );

        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0x09),
                test.secondIsPair(REG_PAIR_DE).run(0x19),
                test.secondIsPair(REG_SP).run(0x39)
        );
    }

    /* 8080-incompatible */

    @Test
    public void testADD_A__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF))
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIX()
                .first8LSBisRegister(REG_A)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0x86)
        );
    }


    @Test
    public void testADD_A__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF))
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIY()
                .first8LSBisRegister(REG_A)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0x86)
        );
    }

    @Test
    public void testADC_A__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & FLAG_C))
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIX()
                .first8LSBisRegister(REG_A)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0x8E)
        );
    }

    @Test
    public void testADC_A__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flags & FLAG_C))
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIY()
                .first8LSBisRegister(REG_A)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsReset())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0x8E)
        );
    }

    @Test
    public void testSUB_A__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIX()
                .first8LSBisRegister(REG_A)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0x96)
        );
    }

    @Test
    public void testSUB_A__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIY()
                .first8LSBisRegister(REG_A)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0x96)
        );
    }

    @Test
    public void testSBC_A__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & FLAG_C))
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIX()
                .first8LSBisRegister(REG_A)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0x9E)
        );
    }

    @Test
    public void testSBC_A__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIY()
                .first8LSBisRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flags & FLAG_C))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().halfCarry().overflow().subtractionIsSet())
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0x9E)
        );
    }

    @Test
    public void testINC__IX() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIX()
                .verifyIX(context -> context.first + 1)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>());

        Generator.forSome16bitUnary(
                test.run(0xDD, 0x23)
        );
    }

    @Test
    public void testINC__IY() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIY()
                .verifyIY(context -> context.first + 1)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>());

        Generator.forSome16bitUnary(
                test.run(0xFD, 0x23)
        );
    }

    @Test
    public void testDEC__IX() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIX()
                .verifyIX(context -> context.first - 1)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>());

        Generator.forSome16bitUnary(
                test.run(0xDD, 0x2B)
        );
    }

    @Test
    public void testDEC__IY() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIY()
                .verifyIY(context -> context.first - 1)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>());

        Generator.forSome16bitUnary(
                test.run(0xFD, 0x2B)
        );
    }

    @Test
    public void testINC__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIX()
                .verifyByte(context -> get8MSBplus8LSB(context.first), context -> (context.second + 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                                .switchFirstAndSecond().sign().zero().halfCarry().overflow().subtractionIsReset()
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0x34)
        );
    }

    @Test
    public void testINC__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIY()
                .verifyByte(context -> get8MSBplus8LSB(context.first), context -> (context.second + 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                                .switchFirstAndSecond().sign().zero().halfCarry().overflow().subtractionIsReset()
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0x34)
        );
    }

    @Test
    public void testDEC__IX_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIX()
                .verifyByte(context -> get8MSBplus8LSB(context.first), context -> (context.second - 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                                .switchFirstAndSecond().sign().zero().halfCarry().overflow().subtractionIsSet()
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xDD, 0x35)
        );
    }

    @Test
    public void testDEC__IY_plus_d() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                .first8MSBisIY()
                .verifyByte(context -> get8MSBplus8LSB(context.first), context -> (context.second - 1))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                                .switchFirstAndSecond().sign().zero().halfCarry().overflow().subtractionIsSet()
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                test.runWithFirst8bitOperand(0xFD, 0x35)
        );
    }

    @Test
    public void testADD_IX__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIX()
                .verifyIX(context -> context.first + context.second)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                                .halfCarry11().carry15().subtractionIsReset()
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
                test.run(0xDD, 0x29)
        );

        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0xDD, 0x09),
                test.secondIsPair(REG_PAIR_DE).run(0xDD, 0x19),
                test.secondIsPair(REG_SP).run(0xDD, 0x39)
        );
    }

    @Test
    public void testADD_IY__ss() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIY()
                .verifyIY(context -> context.first + context.second)
                .verifyFlagsOfLastOp(new FlagsBuilderImpl<>()
                                .halfCarry11().carry15().subtractionIsReset()
                )
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
                test.run(0xFD, 0x29)
        );

        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0xFD, 0x09),
                test.secondIsPair(REG_PAIR_DE).run(0xFD, 0x19),
                test.secondIsPair(REG_SP).run(0xFD, 0x39)
        );
    }

    @Test
    public void testADC_HL__ss() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyPair(REG_PAIR_HL, context -> context.first + context.second + (context.flags & FLAG_C))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl()
                        .sign16bit().zero16bit().overflow16bit().carry15().halfCarry11().subtractionIsReset())
                .firstIsPair(REG_PAIR_HL)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
                test.run(0xED, 0x6A)
        );

        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0xED, 0x4A),
                test.secondIsPair(REG_PAIR_DE).run(0xED, 0x5A),
                test.secondIsPair(REG_SP).run(0xED, 0x7A)
        );
    }

    @Test
    public void testSBC_HL__ss() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyPair(REG_PAIR_HL, context ->
                        (context.first + ((-context.second - (context.flags & FLAG_C)) & 0xFFFF)))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl()
                        .sign16bit().zero16bit().overflow16bit().carry15().halfCarry11().subtractionIsSet())
                .firstIsPair(REG_PAIR_HL)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinaryWhichEqual(
                test.run(0xED, 0x62)
        );

        Generator.forSome16bitBinary(
                test.secondIsPair(REG_PAIR_BC).run(0xED, 0x42),
                test.secondIsPair(REG_PAIR_DE).run(0xED, 0x52),
                test.secondIsPair(REG_SP).run(0xED, 0x72)
        );
    }

}
