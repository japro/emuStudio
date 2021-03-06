/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.zilogZ80.impl;

import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.cpu.testsuite.RunnerContext;
import net.sf.emustudio.zilogZ80.impl.suite.ByteTestBuilder;
import net.sf.emustudio.zilogZ80.impl.suite.FlagsCheckImpl;
import net.sf.emustudio.zilogZ80.impl.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.BiFunction;
import java.util.function.Function;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.*;

public class IOTest extends InstructionsTest {

    @Test
    public void testIN_A__n() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsDeviceAndSecondIsPort()
            .secondIsRegister(REG_A)
            .verifyRegister(REG_A, context -> context.first & 0xFF);

        Generator.forSome8bitBinary(
            test.runWithSecondOperand(0xDB)
        );
    }

    @Test
    public void testIN_r__C() {
        Function<RunnerContext<Byte>, Integer> operation = context -> context.first & 0xFF;

        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsDeviceAndSecondIsPort()
            .secondIsRegister(REG_C)
            .verifyFlags(new FlagsCheckImpl<>().sign().zero().parity().halfCarryIsReset().subtractionIsReset(),
                operation)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forSome8bitBinary(
            test.verifyRegister(REG_A, operation).runWithSecondOperand(0xED, 0x78),
            test.verifyRegister(REG_B, operation).runWithSecondOperand(0xED, 0x40),
            test.verifyRegister(REG_C, operation).runWithSecondOperand(0xED, 0x48),
            test.verifyRegister(REG_D, operation).runWithSecondOperand(0xED, 0x50),
            test.verifyRegister(REG_E, operation).runWithSecondOperand(0xED, 0x58),
            test.verifyRegister(REG_H, operation).runWithSecondOperand(0xED, 0x60),
            test.verifyRegister(REG_L, operation).runWithSecondOperand(0xED, 0x68)
        );
    }

    @Test
    public void testIN__C() {
        Function<RunnerContext<Byte>, Integer> operation = context -> context.first & 0xFF;

        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsDeviceAndSecondIsPort()
            .secondIsRegister(REG_C)
            .verifyFlags(new FlagsCheckImpl<>().sign().zero().parity().halfCarryIsReset().subtractionIsReset(),
                operation)
            .keepCurrentInjectorsAfterRun()
            .clearOtherVerifiersAfterRun();

        Generator.forSome8bitBinary(
            test.runWithSecondOperand(0xED, 0x70)
        );
    }

    @Test
    public void testINI() {
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagHCtest = (context, result) ->
            (((context.first >>> 8) & 0xFF) + ((context.first & 0xFF + 1) & 0xFF)) > 0xFF;
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagPVtest = (context, result) ->
            FlagsCheckImpl.isParity(
                ((((context.first >>> 8) & 0xFF) + (((context.first & 0xFF) + 1) & 0xFF)) & 7) ^ result.byteValue()
            );

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisDeviceAndFirst8LSBIsPort()
            .first8LSBisRegister(REG_C)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .first8MSBisRegister(REG_B)
            .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
            .verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
            .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero()
                .expectFlagOnlyWhen(FLAG_N,
                    (context, result) -> (((context.first >>> 8) & 0x80) == 0x80)
                )
                .expectFlagOnlyWhen(FLAG_H, flagHCtest)
                .expectFlagOnlyWhen(FLAG_C, flagHCtest)
                .expectFlagOnlyWhen(FLAG_PV, flagPVtest)
            );

        Generator.forSome16bitBinary(2,
            test.run(0xED, 0xA2)
        );
    }

    @Test
    public void testINIR() {
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagHCtest = (context, result) ->
            (((context.first >>> 8) & 0xFF) + ((context.first & 0xFF + 1) & 0xFF)) > 0xFF;
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagPVtest = (context, result) ->
            FlagsCheckImpl.isParity(
                ((((context.first >>> 8) & 0xFF) + (((context.first & 0xFF) + 1) & 0xFF)) & 7) ^ result.byteValue()
            );

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisDeviceAndFirst8LSBIsPort()
            .first8LSBisRegister(REG_C)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .first8MSBisRegister(REG_B)
            .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
            .verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
            .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero()
                .expectFlagOnlyWhen(FLAG_N,
                    (context, result) -> (((context.first >>> 8) & 0x80) == 0x80)
                )
                .expectFlagOnlyWhen(FLAG_H, flagHCtest)
                .expectFlagOnlyWhen(FLAG_C, flagHCtest)
                .expectFlagOnlyWhen(FLAG_PV, flagPVtest)
            )
            .verifyPC(context -> {
                if (((((context.first >>> 8) & 0xFF) - 1) & 0xFF) != 0) {
                    return context.PC;
                }
                return context.PC + 2;
            });

        Generator.forSome16bitBinary(2,
            test.run(0xED, 0xB2)
        );
    }

    @Test
    public void testIND() {
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagHCtest = (context, result) ->
            (((context.first >>> 8) & 0xFF) + (((context.first & 0xFF) - 1) & 0xFF)) > 0xFF;
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagPVtest = (context, result) ->
            FlagsCheckImpl.isParity(
                ((((context.first >>> 8) & 0xFF) + (((context.first & 0xFF) - 1) & 0xFF)) & 7) ^ result.byteValue()
            );

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisDeviceAndFirst8LSBIsPort()
            .first8LSBisRegister(REG_C)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .first8MSBisRegister(REG_B)
            .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
            .verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF)
            .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero()
                .expectFlagOnlyWhen(FLAG_N,
                    (context, result) -> (((context.first >>> 8) & 0x80) == 0x80)
                )
                .expectFlagOnlyWhen(FLAG_H, flagHCtest)
                .expectFlagOnlyWhen(FLAG_C, flagHCtest)
                .expectFlagOnlyWhen(FLAG_PV, flagPVtest)
            );

        Generator.forSome16bitBinary(2,
            test.run(0xED, 0xAA)
        );
    }

    @Test
    public void testINDR() {
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagHCtest = (context, result) ->
            (((context.first >>> 8) & 0xFF) + (((context.first & 0xFF) - 1) & 0xFF)) > 0xFF;
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagPVtest = (context, result) ->
            FlagsCheckImpl.isParity(
                ((((context.first >>> 8) & 0xFF) + (((context.first & 0xFF) - 1) & 0xFF)) & 7) ^ result.byteValue()
            );

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .first8MSBisDeviceAndFirst8LSBIsPort()
            .first8LSBisRegister(REG_C)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .first8MSBisRegister(REG_B)
            .verifyByte(context -> context.first, context -> (context.first >>> 8) & 0xFF)
            .verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF)
            .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero()
                .expectFlagOnlyWhen(FLAG_N,
                    (context, result) -> (((context.first >>> 8) & 0x80) == 0x80)
                )
                .expectFlagOnlyWhen(FLAG_H, flagHCtest)
                .expectFlagOnlyWhen(FLAG_C, flagHCtest)
                .expectFlagOnlyWhen(FLAG_PV, flagPVtest)
            )
            .verifyPC(context -> {
                if (((((context.first >>> 8) & 0xFF) - 1) & 0xFF) != 0) {
                    return context.PC;
                }
                return context.PC + 2;
            });

        Generator.forSome16bitBinary(2,
            test.run(0xED, 0xBA)
        );
    }

    @Test
    public void testOUT_n__A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsRegister(REG_A)
            .verifyDeviceWhenSecondIsPort(context -> context.first & 0xFF);

        Generator.forSome8bitBinary(
            test.runWithSecondOperand(0xD3)
        );
    }

    @Test
    public void testOUT_C__r() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .secondIsRegister(REG_C)
            .verifyDeviceWhenSecondIsPort(context -> context.first & 0xFF)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
            test.firstIsRegister(REG_C).run(0xED, 0x49)
        );

        Generator.forSome8bitBinary(
            test.firstIsRegister(REG_B).run(0xED, 0x41),
            test.firstIsRegister(REG_D).run(0xED, 0x51),
            test.firstIsRegister(REG_E).run(0xED, 0x59),
            test.firstIsRegister(REG_H).run(0xED, 0x61),
            test.firstIsRegister(REG_L).run(0xED, 0x69),
            test.firstIsRegister(REG_A).run(0xED, 0x79)
        );
    }

    @Test
    public void testOUT_C_0() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .secondIsRegister(REG_C)
            .verifyDeviceWhenSecondIsPort(context -> 0)
            .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitBinaryWhichEqual(
            test.firstIsRegister(REG_C).run(0xED, 0x71)
        );
    }

    @Test
    public void testOUTI() {
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagHCtest = (context, result) ->
            ((context.second & 0xFF) + ((context.first+1) & 0xFF)) > 0xFF;
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagPVtest = (context, result) ->
            FlagsCheckImpl.isParity(
                (((context.second & 0xFF) + ((context.first+1) & 0xFF)) & 7) ^ result.byteValue()
            );

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .first8LSBisRegister(REG_C)
            .first8MSBisRegister(REG_B)
            .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
            .verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
            .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero()
                .expectFlagOnlyWhen(FLAG_N,
                    (context, result) -> ((context.second & 0x80) == 0x80)
                )
                .expectFlagOnlyWhen(FLAG_H, flagHCtest)
                .expectFlagOnlyWhen(FLAG_C, flagHCtest)
                .expectFlagOnlyWhen(FLAG_PV, flagPVtest)
            );

        Generator.forSome16bitBinary(2,
            test.run(0xED, 0xA3)
        );
    }

    @Test
    public void testOTIR() {
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagHCtest = (context, result) ->
            ((context.second & 0xFF) + ((context.first + 1) & 0xFF)) > 0xFF;
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagPVtest = (context, result) ->
            FlagsCheckImpl.isParity(
                (((context.second & 0xFF) + (context.first+1) & 0xFF) & 7) ^ result.byteValue()
            );

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .first8LSBisRegister(REG_C)
            .first8MSBisRegister(REG_B)
            .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
            .verifyPair(REG_PAIR_HL, context -> (context.first + 1) & 0xFFFF)
            .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero()
                .expectFlagOnlyWhen(FLAG_N,
                    (context, result) -> ((context.second & 0x80) == 0x80)
                )
                .expectFlagOnlyWhen(FLAG_H, flagHCtest)
                .expectFlagOnlyWhen(FLAG_C, flagHCtest)
                .expectFlagOnlyWhen(FLAG_PV, flagPVtest)
            )
            .verifyPC(context -> {
                if (((((context.first >>> 8) & 0xFF) - 1) & 0xFF) != 0) {
                    return context.PC;
                }
                return context.PC + 2;
            });

        Generator.forSome16bitBinary(2,
            test.run(0xED, 0xB3)
        );
    }

    @Test
    public void testOUTD() {
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagHCtest = (context, result) ->
            ((context.second & 0xFF) + ((context.first-1) & 0xFF)) > 0xFF;
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagPVtest = (context, result) ->
            FlagsCheckImpl.isParity(
                (((context.second & 0xFF) + ((context.first-1) & 0xFF)) & 7) ^ result.byteValue()
            );

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .first8LSBisRegister(REG_C)
            .first8MSBisRegister(REG_B)
            .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
            .verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF)
            .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero()
                .expectFlagOnlyWhen(FLAG_N,
                    (context, result) -> ((context.second & 0x80) == 0x80)
                )
                .expectFlagOnlyWhen(FLAG_H, flagHCtest)
                .expectFlagOnlyWhen(FLAG_C, flagHCtest)
                .expectFlagOnlyWhen(FLAG_PV, flagPVtest)
            );

        Generator.forSome16bitBinary(2,
            test.run(0xED, 0xAB)
        );
    }

    @Test
    public void testOTDR() {
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagHCtest = (context, result) ->
            ((context.second & 0xFF) + ((context.first-1) & 0xFF)) > 0xFF;
        BiFunction<RunnerContext<Integer>, Number, Boolean> flagPVtest = (context, result) ->
            FlagsCheckImpl.isParity(
                (((context.second & 0xFF) + ((context.first-1) & 0xFF)) & 7) ^ result.byteValue()
            );

        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
            .firstIsAddressAndSecondIsMemoryByte()
            .firstIsPair(REG_PAIR_HL)
            .first8LSBisRegister(REG_C)
            .first8MSBisRegister(REG_B)
            .verifyDeviceWhenFirst8LSBisPort(context -> context.second & 0xFF)
            .verifyPair(REG_PAIR_HL, context -> (context.first - 1) & 0xFFFF)
            .verifyRegister(REG_B, context -> (((context.first >>> 8) & 0xFF) - 1) & 0xFF)
            .verifyFlagsOfLastOp(new FlagsCheckImpl<Integer>().sign().zero()
                .expectFlagOnlyWhen(FLAG_N,
                    (context, result) -> ((context.second & 0x80) == 0x80)
                )
                .expectFlagOnlyWhen(FLAG_H, flagHCtest)
                .expectFlagOnlyWhen(FLAG_C, flagHCtest)
                .expectFlagOnlyWhen(FLAG_PV, flagPVtest)
            )
            .verifyPC(context -> {
                if (((((context.first >>> 8) & 0xFF) - 1) & 0xFF) != 0) {
                    return context.PC;
                }
                return context.PC + 2;
            });

        Generator.forSome16bitBinary(2,
            test.run(0xED, 0xBB)
        );
    }
}
    
