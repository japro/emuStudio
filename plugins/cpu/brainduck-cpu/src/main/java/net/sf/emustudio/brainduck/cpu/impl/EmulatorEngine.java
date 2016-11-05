package net.sf.emustudio.brainduck.cpu.impl;

import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

public class EmulatorEngine {
    public final static short I_STOP = 0; // ;
    public final static short I_INC = 1; // >
    public final static short I_DEC = 2; // <
    public final static short I_INCV = 3; // +
    public final static short I_DECV = 4; // -
    public final static short I_PRINT = 5; // .
    public final static short I_READ = 6; // ,
    public final static short I_LOOP_START = 7; // [
    public final static short I_LOOP_END = 8; // ]
    public final static short I_CLEAR = 0xA1; // [-]
    public final static short I_COPY_INC_FORWARD_AND_CLEAR = 0xA2; // [(-)... >+ ...  <(-)]
    public final static short I_COPY_DEC_FORWARD_AND_CLEAR = 0xA3; // [(-)... >- ...  <(-)]
    public final static short I_COPY_INC_BACKWARD_AND_CLEAR = 0xA4; // [(-)... <+ ... >(-)]
    public final static short I_COPY_DEC_BACKWARD_AND_CLEAR = 0xA5; // [(-)... <- ... >(-)]

    private final MemoryContext<Short> memory;
    private final int memorySize;
    private final BrainCPUContextImpl context;
    private final Deque<Integer> loopPointers = new LinkedList<>();
    private final Profiler profiler;

    public volatile int IP, P; // registers of the CPU

    public EmulatorEngine(MemoryContext<Short> memory, BrainCPUContextImpl context, Profiler profiler) {
        this.memory = Objects.requireNonNull(memory);
        this.memorySize = memory.getSize();
        this.context = Objects.requireNonNull(context);
        this.profiler = Objects.requireNonNull(profiler);
    }

    public void reset(int adr) {
        IP = adr; // initialize program counter

        // find closest "free" address which does not contain a program
        try {
            while (memory.read(adr++) != 0) {
            }
        } catch (IndexOutOfBoundsException e) {
            // we get here if "adr" would point to nonexistant memory location,
            // ie. when we go through all memory to the end without a result
            adr = 0;
        }
        P = adr; // assign to the P register the address we have found
        profileAndOptimize(adr);
    }

    private void profileAndOptimize(int programSize) {
        profiler.optimizeRepeatingOperations(programSize);
        profiler.optimizeCopyLoops(programSize);
        profiler.optimizeLoops(programSize);
    }

    public int getP() {
        return P;
    }


    public CPU.RunState step(boolean optimize) throws IOException {
        short OP;

        // FETCH
        int argument = 1;

        Profiler.CachedOperation operation = profiler.findCachedOperation(IP);
        if (optimize && operation != null) {
            OP = operation.operation;
            IP = operation.nextIP;
            argument = operation.argument;
        } else {
            OP = memory.read(IP++);
        }

        // DECODE
        switch (OP) {
            case I_STOP: /* ; */
                return CPU.RunState.STATE_STOPPED_NORMAL;
            case I_INC: /* >  */
                P += argument;
                if (P > memorySize) {
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                }
                break;
            case I_DEC: /* < */
                P -= argument;
                if (P < 0) {
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                }
                break;
            case I_INCV: /* + */
                memory.write(P, (short) (memory.read(P) + argument));
                break;
            case I_DECV: /* - */
                memory.write(P, (short) (memory.read(P) - argument));
                break;
            case I_PRINT: /* . */
                while (argument > 0) {
                    context.writeToDevice(memory.read(P));
                    argument--;
                }
                break;
            case I_READ: /* , */
                while (argument > 0) {
                    memory.write(P, context.readFromDevice());
                    argument--;
                }
                break;
            case I_LOOP_START: /* [ */
                int startingBrace = IP - 1;
                if (memory.read(P) != 0) {
                    loopPointers.push(startingBrace);
                    break;
                }
                IP = profiler.findLoopEnd(startingBrace);
                break;
            case I_LOOP_END: /* ] */
                int tmpIP = loopPointers.pop();
                if (memory.read(P) != 0) {
                    IP = tmpIP;
                }
                break;
            case I_CLEAR: /* [-] */
                memory.write(P, (short)0);
                break;
            case I_COPY_INC_FORWARD_AND_CLEAR: /* [>+<-] */
                for (int i = 0; i < argument; i++) {
                    memory.write(P + 1 + i, (short)(memory.read(P) + memory.read(P + 1 + i)));
                }
                memory.write(P, (short)0);
                break;
            case I_COPY_INC_BACKWARD_AND_CLEAR: /* [<+>-] */
                for (int i = 0; i < argument; i++) {
                    memory.write(P - 1 - i, (short)(memory.read(P) + memory.read(P - 1 - i)));
                }
                memory.write(P, (short)0);
                break;
            case I_COPY_DEC_FORWARD_AND_CLEAR: /* [>-<-] */
                for (int i = 0; i < argument; i++) {
                    memory.write(P + 1 + i, (short)(memory.read(P + 1 + i) - memory.read(P)));
                }
                memory.write(P, (short)0);
                break;
            case I_COPY_DEC_BACKWARD_AND_CLEAR: /* [<->-] */
                for (int i = 0; i < argument; i++) {
                    memory.write(P - 1 - i, (short)(memory.read(P - 1 - i) - memory.read(P)));
                }
                memory.write(P, (short)0);
                break;
            default: /* invalid instruction */
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    public int getLoopLevel() {
        return loopPointers.size();
    }

}