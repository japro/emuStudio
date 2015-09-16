package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.RunnerInjector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstructionOperand<K extends Number, CpuRunnerType extends CpuRunner>
        implements RunnerInjector<K, CpuRunnerType> {
    private final List<Integer> opcodes;
    private final List<Integer> opcodesAfterOperand = new ArrayList<>();

    public InstructionOperand(int... opcodes) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : opcodes) {
            tmpList.add(opcode);
        }
        this.opcodes = Collections.unmodifiableList(tmpList);
    }

    public InstructionOperand placeOpcodesAfterOperand(int... opcodes) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : opcodes) {
            tmpList.add(opcode);
        }
        opcodesAfterOperand.addAll(tmpList);
        return this;
    }

    @Override
    public void inject(CpuRunnerType cpuRunner, K operand) {
        int tmpOperand = operand.intValue() & 0xFFFF;
        List<Integer> program = new ArrayList<>(opcodes);
        if (operand instanceof Byte) {
            program.add(tmpOperand & 0xFF);
        } else {
            program.add(tmpOperand & 0xFF);
            program.add((tmpOperand >>> 8) & 0xFF);
        }
        program.addAll(opcodesAfterOperand);
        cpuRunner.setProgram(program);
        cpuRunner.ensureProgramSize(tmpOperand + 2);
    }
}