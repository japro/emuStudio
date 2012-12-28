/*
 * BrainCPU.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.braincpu.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.JPanel;
import net.sf.emustudio.braincpu.BrainCPUContext;
import net.sf.emustudio.braincpu.gui.BrainStatusPanel;
import net.sf.emustudio.braincpu.gui.DecoderImpl;
import net.sf.emustudio.braincpu.gui.DisassemblerImpl;

@PluginType(type = PLUGIN_TYPE.CPU,
title = "BrainCPU",
copyright = "\u00A9 Copyright 2009-2012, Peter Jakubčo",
description = "Emulator of CPU for abstract BrainDuck architecture")
public class EmulatorImpl extends AbstractCPU {
    private MemoryContext memory;
    private BrainCPUContextImpl context;
    private int IP, P; // registers of the CPU
    private Disassembler disassembler;

    public EmulatorImpl(Long pluginID) {
        super(pluginID);
        context = new BrainCPUContextImpl();
        try {
            ContextPool.getInstance().register(pluginID, context, BrainCPUContext.class);
        } catch (RuntimeException e) {
            StaticDialogs.showErrorMessage("Could not register CPU Context",
                    EmulatorImpl.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.braincpu.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public boolean initialize(SettingsManager settings) {
        super.initialize(settings);
        memory = ContextPool.getInstance().getMemoryContext(pluginID, MemoryContext.class);

        if (memory == null) {
            StaticDialogs.showErrorMessage("BrainCPU must have access to memory");
            return false;
        }

        if (memory.getDataType() != Short.class) {
            StaticDialogs.showErrorMessage("Operating memory type is not supported"
                    + " for this kind of CPU.");
            return false;
        }
        disassembler = new DisassemblerImpl(memory, new DecoderImpl(memory));
        return true;
    }

    @Override
    public int getInstructionPosition() {
        return IP;
    }

    @Override
    public boolean setInstructionPosition(int pos) {
        if (pos < 0) {
            return false;
        }
        IP = pos;
        return true;
    }

    @Override
    public JPanel getStatusPanel() {
        return new BrainStatusPanel(this, memory);
    }

    @Override
    public void showSettings() {
        // We don't use any GUI for the settings
    }

    public int getP() {
        return P;
    }

    public int getIP() {
        return IP;
    }

    @Override
    public void reset(int adr) {
        super.reset(adr);
        IP = adr; // initialize program counter

        // find closest "free" address which does not contain a program
        try {
            while ((Short) memory.read(adr++) != 0) {
            }
        } catch (IndexOutOfBoundsException e) {
            // we get here if "adr" would point to nonexistant memory location,
            // ie. when we go through all memory to the end without a result
            adr = 0;
        }
        P = adr; // assign to the P register the address we have found

        // notify new CPU state
        notifyCPURunState(runState);
        notifyCPUState();
    }

    @Override
    public void run() {
        // change the CPU state to "running"
        runState = RunState.STATE_RUNNING;
        // notify the state
        notifyCPURunState(runState);

        // here we are emulating in endless loop until an event stops it.
        // Externally, it might be the user, internally invalid instruction
        // or nonexistant memory location (address fallout)
        while (runState == RunState.STATE_RUNNING) {
            try {
                // if a breakpoint is set on the address pointed by IP register
                // we throw new Breakpoint instance.
                if (isBreakpointSet(IP) == true) {
                    throw new Breakpoint();
                }
                emulateInstruction();
            } catch (IndexOutOfBoundsException e) {
                // we get here if IP registers would point at nonexistant memory
                runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
                break;
            } catch (Breakpoint er) {
                // we get here if a Breakpoint was thrown.
                runState = RunState.STATE_STOPPED_BREAK;
                break;
            }
        }
        notifyCPUState();
        notifyCPURunState(runState);
    }

    @Override
    public void pause() {
        // change run state to "breakpoint"
        runState = RunState.STATE_STOPPED_BREAK;
        // notify the new state
        notifyCPURunState(runState);
    }

    @Override
    public void step() {
        // if the run state is "breakpoint"
        if (runState == RunState.STATE_STOPPED_BREAK) {
            try {
                // change the state to "running"
                runState = RunState.STATE_RUNNING;
                emulateInstruction();
                // if the emulation would like to continue (if it wasn't
                // interrupted externally nor internally)
                if (runState == RunState.STATE_RUNNING) {
                    // then we change the run state back to "breakpoint"
                    runState = RunState.STATE_STOPPED_BREAK;
                }
            } catch (IndexOutOfBoundsException e) {
                // we get here if IP register would point at nonexistant memory
                runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
            // notify CPU run state
            notifyCPURunState(runState);
            notifyCPUState();
        }
    }

    @Override
    public void stop() {
        // change run state to "stopped"
        runState = RunState.STATE_STOPPED_NORMAL;
        // notify the state
        notifyCPURunState(runState);
    }

    @Override
    public void destroy() {
        runState = RunState.STATE_STOPPED_NORMAL;
    }

    /**
     * This method emulates a single instruction.
     */
    private void emulateInstruction() {
        short OP, param;

        // FETCH
        OP = ((Short) memory.read(IP++)).shortValue();

        // DECODE
        switch (OP) {
            case 0: /* HALT */
                runState = RunState.STATE_STOPPED_NORMAL;
                return;
            case 1: /* INC */
                param = ((Short) memory.read(IP++)).shortValue();
                if (param == 0xff) {
                    P++;
                } else {
                    while (param > 0) {
                        P++;
                        param--;
                    }
                }
                return;
            case 2: /* DEC */
                param = ((Short) memory.read(IP++)).shortValue();
                if (param == 0xff) {
                    P--;
                } else {
                    while (param > 0) {
                        P--;
                        param--;
                    }
                }
                return;
            case 3: /* INCV */
                param = ((Short) memory.read(IP++)).shortValue();
                if (param == 0xff) {
                    memory.write(P, (Short) memory.read(P) + 1);
                } else {
                    while (param > 0) {
                        memory.write(P, (Short) memory.read(P) + 1);
                        param--;
                    }
                }
                return;
            case 4: /* DECV */
                param = ((Short) memory.read(IP++)).shortValue();
                if (param == 0xff) {
                    memory.write(P, (Short) memory.read(P) - 1);
                } else {
                    while (param > 0) {
                        memory.write(P, (Short) memory.read(P) - 1);
                        param--;
                    }
                }
                return;
            case 5: /* PRINT */
                param = ((Short) memory.read(IP++)).shortValue();
                if (param == 0xff) {
                    context.writeToDevice((Short) memory.read(P));
                } else {
                    while (param > 0) {
                        context.writeToDevice((Short) memory.read(P));
                        param--;
                    }
                }
                return;
            case 6: /* LOAD */
                param = ((Short) memory.read(IP++)).shortValue();
                if (param == 0xff) {
                    memory.write(P, context.readFromDevice());
                } else {
                    while (param > 0) {
                        memory.write(P, context.readFromDevice());
                        P++;
                        param--;
                    }
                }
                return;
            case 7: { /* LOOP */
                if ((Short) memory.read(P) != 0) {
                    return;
                }
                byte loop_count = 0; // loop nesting level counter
                
                // We might have only 127 nesting levels at max.
                // If this is broken, then we start to look for "endl" instruction
                // on the same nesting level (according to loop_count value)
                // IP is pointing at following instruction
                while ((OP = (Short) memory.read(IP++)) != 0) {
                    // if the instruction is in the range <0,6> then it has a parameter
                    if ((OP > 0) && (OP <= 6)) {
                        if ((OP = (Short) memory.read(IP++)) == 0) {
                            break;
                        }
                    }
                    if (OP == 7) {
                        loop_count++;
                    }
                    if (OP == 8) {
                        if (loop_count == 0) {
                            return;
                        } else {
                            loop_count--;
                        }
                    }
                }
                // we get here if some error happened (we didn't reached "endl")
                break;
            }
            case 8: /* ENDL */
                if ((Short) memory.read(P) == 0) {
                    return;
                }
                short old_OP;
                byte loop_count = 0; // the same meaning as in the "loop" instruction
                IP--; // set IP to point one instruction back
                while ((OP = (Short) memory.read(--IP)) != 0) {
                    if (IP - 1 >= 0) {
                        old_OP = (Short) memory.read(IP - 1);
                        if (old_OP > 0 && old_OP <= 6) {
                            OP = old_OP;
                            IP--;
                        }
                    }
                    if (OP == 8) {
                        loop_count++;
                    }
                    if (OP == 7) {
                        if (loop_count == 0) {
                            return;
                        } else {
                            loop_count--;
                        }
                    }
                }
                // we didn't reached "loop"
                break;
            default: /* invalid instruction */
                break;
        }
        runState = RunState.STATE_STOPPED_BAD_INSTR;
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }
}
