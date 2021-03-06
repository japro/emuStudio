package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.Plugin;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.intel8080.api.DefaultInitializer;
import net.sf.emustudio.intel8080.api.DispatchListener;
import net.sf.emustudio.intel8080.gui.DecoderImpl;
import net.sf.emustudio.intel8080.gui.DisassemblerImpl;

import java.util.Objects;

public class InitializerFor8080 extends DefaultInitializer<EmulatorEngine> {
    private final ContextImpl context;


    public InitializerFor8080(Plugin plugin, long pluginId, ContextPool contextPool, SettingsManager settings, ContextImpl context)
        throws PluginInitializationException {
        super(plugin, pluginId, contextPool, settings);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    protected EmulatorEngine createEmulatorEngine(MemoryContext memory) {
        return new EmulatorEngine(memory, context);
    }

    @Override
    protected DispatchListener createInstructionPrinter(Disassembler disassembler, EmulatorEngine engine, boolean useCache) {
        return new InstructionPrinter(disassembler, engine, useCache);
    }

    @Override
    protected Disassembler createDisassembler(MemoryContext<Short> memory) {
        return new DisassemblerImpl(memory, new DecoderImpl(memory));
    }
}
