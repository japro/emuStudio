/*
 * StudioFrame.java
 *
 * Created on Nedeľa, 2007, august 5, 13:43
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package emustudio.gui;

import emustudio.architecture.Computer;
import emustudio.main.Main;
import emustudio.gui.utils.DebugTable;
import emustudio.gui.utils.DebugTableModel;
import emustudio.gui.utils.NiceButton;
import emustudio.gui.utils.EmuTextPane;
import emustudio.gui.utils.FindText;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.StringReader;
import java.util.EventObject;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import emuLib8.plugins.compiler.ICompiler;
import emuLib8.plugins.compiler.ICompiler.ICompilerListener;
import emuLib8.plugins.compiler.Message;
import emuLib8.plugins.cpu.ICPU;
import emuLib8.plugins.cpu.ICPU.RunState;
import emuLib8.plugins.device.IDevice;
import emuLib8.plugins.memory.IMemory;
import emuLib8.plugins.memory.IMemory.IMemListener;
import emuLib8.runtime.StaticDialogs;

/**
 * The main window class
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class StudioFrame extends javax.swing.JFrame {
    private EmuTextPane txtSource;
    private Computer arch; // current architecture
    private ActionListener undoStateListener;
    private Clipboard systemClipboard;
    private RunState run_state = RunState.STATE_STOPPED_BREAK;
    private DebugTable tblDebug;
    // emulator
    private DebugTableModel debug_model;
    private ICompiler compiler;
    private IMemory memory;
    private ICPU cpu;

    /**
     * Create new instance of the main window frame.
     *
     * @param fileName file name to open in the source code editor
     * @param title title of the main window
     */
    public StudioFrame(String fileName, String title) {
        this(title);
        txtSource.openFile(fileName);
    }

    /**
     * Creates new instance of the main window frame.
     *
     * @param title title of the main window
     */
    public StudioFrame(String title) {
        // create models and components
        arch = Main.currentArch.getComputer();
        txtSource = new EmuTextPane();
        debug_model = new DebugTableModel(arch.getCPU(), arch.getMemory());
        tblDebug = new DebugTable(debug_model);
        initComponents();
        btnBreakpoint.setEnabled(arch.getCPU().isBreakpointSupported());
        jScrollPane1.setViewportView(txtSource);
        paneDebug.setViewportView(tblDebug);

        compiler = arch.getCompiler();
        memory = arch.getMemory();
        cpu = arch.getCPU();

        if (compiler == null) {
            btnCompile.setEnabled(false);
            mnuProjectCompile.setEnabled(false);
        }

        this.setStatusGUI();
        setupListeners();

        btnBreakpoint.setEnabled(cpu.isBreakpointSupported());
        lstDevices.setModel(new AbstractListModel() {

            @Override
            public int getSize() {
                return arch.getDevices().length;
            }

            @Override
            public Object getElementAt(int index) {
                return arch.getDevices()[index].getTitle();
            }
        });
        this.setLocationRelativeTo(null);
        this.setTitle("emuStudio - " + title);
        txtSource.grabFocus();
    }

    // get gui panel from CPU plugin and show in main window
    private void setStatusGUI() {
        JPanel statusPanel = cpu.getStatusGUI();
        if (statusPanel == null) {
            return;
        }
        GroupLayout layout = new GroupLayout(this.statusWindow);
        this.statusWindow.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(statusPanel));
        layout.setVerticalGroup(
                layout.createSequentialGroup().addComponent(statusPanel));
        pack();
    }

    private void setupListeners() {
        if (compiler != null) {
            compiler.addCompilerListener(new ICompilerListener() {

                @Override
                public void onStart(EventObject evt) {
                }

                @Override
                public void onMessage(EventObject evt, Message message) {
                    txtOutput.append(message.getForrmattedMessage() + "\n");
                }

                @Override
                public void onFinish(EventObject evt, int errorCode) {
                }
            });
            txtSource.setLexer(compiler.getLexer(txtSource.getDocumentReader()));
            if (!compiler.isShowSettingsSupported()) {
                mnuProjectCompilerSettings.setEnabled(false);
            }
        } else {
            mnuProjectCompilerSettings.setEnabled(false);
        }
        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (systemClipboard.getContents(null) != null) {
            btnPaste.setEnabled(true);
            mnuEditPaste.setEnabled(true);
        }
        systemClipboard.addFlavorListener(new FlavorListener() {

            @Override
            public void flavorsChanged(FlavorEvent e) {
                if (systemClipboard.getContents(null) == null) {
                    btnPaste.setEnabled(false);
                    mnuEditPaste.setEnabled(false);
                } else {
                    btnPaste.setEnabled(true);
                    mnuEditPaste.setEnabled(true);
                }
            }
        });
        txtSource.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                if (e.getDot() == e.getMark()) {
                    btnCut.setEnabled(false);
                    mnuEditCut.setEnabled(false);
                    btnCopy.setEnabled(false);
                    mnuEditCopy.setEnabled(false);
                } else {
                    btnCut.setEnabled(true);
                    mnuEditCut.setEnabled(true);
                    btnCopy.setEnabled(true);
                    mnuEditCopy.setEnabled(true);
                }
            }
        });
        undoStateListener = new ActionListener() {

            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                if (txtSource.canUndo() == true) {
                    mnuEditUndo.setEnabled(true);
                    btnUndo.setEnabled(true);
                } else {
                    mnuEditUndo.setEnabled(false);
                    btnUndo.setEnabled(false);
                }
                if (txtSource.canRedo() == true) {
                    mnuEditRedo.setEnabled(true);
                    btnRedo.setEnabled(true);
                } else {
                    mnuEditRedo.setEnabled(false);
                    btnRedo.setEnabled(false);
                }
            }
        };
        txtSource.setUndoStateChangedAction(undoStateListener);
        if (memory != null) {
            memory.addMemoryListener(new IMemListener() {

                @Override
                public void memChange(EventObject evt, int adr) {
                    if (run_state == RunState.STATE_RUNNING) {
                        return;
                    }
                    tblDebug.revalidate();
                    tblDebug.repaint();
                }
            });
            btnMemory.setEnabled(memory.isShowSettingsSupported());
        } else {
            btnMemory.setEnabled(false);
        }
        cpu.addCPUListener(new ICPU.ICPUListener() {

            @Override
            public void stateUpdated(EventObject evt) {
                tblDebug.revalidate();
                tblDebug.repaint();
            }

            @Override
            public void runChanged(EventObject evt, RunState state) {
                run_state = state;
                if (state == RunState.STATE_RUNNING) {
                    btnStop.setEnabled(true);
                    btnBack.setEnabled(false);
                    btnRun.setEnabled(false);
                    btnStep.setEnabled(false);
                    btnBeginning.setEnabled(false);
                    btnPause.setEnabled(true);
                    btnRunTime.setEnabled(false);
                } else {
                    btnPause.setEnabled(false);
                    if (state == RunState.STATE_STOPPED_BREAK) {
                        btnStop.setEnabled(true);
                        btnRunTime.setEnabled(true);
                        btnRun.setEnabled(true);
                        btnStep.setEnabled(true);
                    } else {
                        btnStop.setEnabled(false);
                        btnRunTime.setEnabled(false);
                        btnRun.setEnabled(false);
                        btnStep.setEnabled(false);
                    }
                    btnBack.setEnabled(true);
                    btnBeginning.setEnabled(true);
                    tblDebug.setEnabled(true);
                    tblDebug.setVisible(true);
                    tblDebug.revalidate();
                    tblDebug.repaint();
                }
            }
        });

    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel panelSource = new JPanel();
        JToolBar toolStandard = new JToolBar();
        JButton btnNew = new JButton();
        JButton btnOpen = new JButton();
        JButton btnSave = new JButton();
        JSeparator jSeparator1 = new JSeparator();
        btnCut = new JButton();
        btnCopy = new JButton();
        btnPaste = new JButton();
        btnFindReplace = new JButton();
        btnUndo = new JButton();
        btnRedo = new JButton();
        JSeparator jSeparator2 = new JSeparator();
        btnCompile = new JButton();
        JSplitPane splitSoure = new JSplitPane();
        jScrollPane1 = new JScrollPane();
        JScrollPane jScrollPane2 = new JScrollPane();
        txtOutput = new JTextArea();
        JPanel panelEmulator = new JPanel();
        JSplitPane splitLeftRight = new JSplitPane();
        statusWindow = new JPanel();
        JSplitPane splitPerDebug = new JSplitPane();
        JPanel debuggerPanel = new JPanel();
        JToolBar toolDebug = new JToolBar();
        JButton btnReset = new JButton();
        btnBeginning = new JButton();
        btnBack = new JButton();
        btnStop = new JButton();
        btnPause = new JButton();
        btnRun = new JButton();
        btnRunTime = new JButton();
        btnStep = new JButton();
        JButton btnJump = new JButton();
        btnBreakpoint = new JButton();
        btnMemory = new JButton();
        paneDebug = new JScrollPane();
        JButton btnPrevious = new NiceButton();
        JButton btnNext = new NiceButton();
        JButton btnToPC = new NiceButton();
        JPanel peripheralPanel = new JPanel();
        JScrollPane paneDevices = new JScrollPane();
        lstDevices = new JList();
        JButton btnShowGUI = new NiceButton();
        final JButton btnShowSettings = new NiceButton();
        JMenuBar jMenuBar2 = new JMenuBar();
        JMenu mnuFile = new JMenu();
        JMenuItem mnuFileNew = new JMenuItem();
        JMenuItem mnuFileOpen = new JMenuItem();
        JSeparator jSeparator3 = new JSeparator();
        JMenuItem mnuFileSave = new JMenuItem();
        JMenuItem mnuFileSaveAs = new JMenuItem();
        JSeparator jSeparator4 = new JSeparator();
        JMenuItem mnuFileExit = new JMenuItem();
        JMenu mnuEdit = new JMenu();
        mnuEditUndo = new JMenuItem();
        mnuEditRedo = new JMenuItem();
        JSeparator jSeparator6 = new JSeparator();
        mnuEditCut = new JMenuItem();
        mnuEditCopy = new JMenuItem();
        mnuEditPaste = new JMenuItem();
        JSeparator jSeparator5 = new JSeparator();
        JMenuItem mnuEditFind = new JMenuItem();
        JMenuItem mnuEditFindNext = new JMenuItem();
        JMenuItem mnuEditReplaceNext = new JMenuItem();
        JMenu mnuProject = new JMenu();
        mnuProjectCompile = new JMenuItem();
        JMenuItem mnuProjectViewConfig = new JMenuItem();
        mnuProjectCompilerSettings = new JMenuItem();
        JMenu mnuHelp = new JMenu();
        JMenuItem mnuHelpAbout = new JMenuItem();
        JSeparator jSeparator7 = new JSeparator();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("emuStudio");
        addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        tabbedPane.setFocusable(false);
        panelSource.setOpaque(false);

        toolStandard.setFloatable(false);
        toolStandard.setRollover(true);

        btnNew.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/document-new.png"))); // NOI18N
        btnNew.setToolTipText("New file");
        btnNew.setFocusable(false);
        btnNew.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });

        btnOpen.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/document-open.png"))); // NOI18N
        btnOpen.setToolTipText("Open file");
        btnOpen.setFocusable(false);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });

        btnSave.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/document-save.png"))); // NOI18N
        btnSave.setToolTipText("Save file");
        btnSave.setFocusable(false);
        btnSave.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jSeparator1.setOrientation(SwingConstants.VERTICAL);
        jSeparator1.setMaximumSize(new java.awt.Dimension(10, 32768));
        jSeparator1.setPreferredSize(new java.awt.Dimension(10, 10));

        btnCut.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/edit-cut.png"))); // NOI18N
        btnCut.setToolTipText("Cut selection");
        btnCut.setEnabled(false);
        btnCut.setFocusable(false);
        btnCut.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCutActionPerformed(evt);
            }
        });

        btnCopy.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/edit-copy.png"))); // NOI18N
        btnCopy.setToolTipText("Copy selection");
        btnCopy.setEnabled(false);
        btnCopy.setFocusable(false);
        btnCopy.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCopyActionPerformed(evt);
            }
        });

        btnPaste.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/edit-paste.png"))); // NOI18N
        btnPaste.setToolTipText("Paste selection");
        btnPaste.setEnabled(false);
        btnPaste.setFocusable(false);
        btnPaste.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPasteActionPerformed(evt);
            }
        });

        btnFindReplace.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/edit-find-replace.png"))); // NOI18N
        btnFindReplace.setToolTipText("Find/replace text...");
        btnFindReplace.setFocusable(false);
        btnFindReplace.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFindReplaceActionPerformed(evt);
            }
        });

        btnUndo.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/edit-undo.png"))); // NOI18N
        btnUndo.setToolTipText("Undo");
        btnUndo.setEnabled(false);
        btnUndo.setFocusable(false);
        btnUndo.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUndoActionPerformed(evt);
            }
        });

        btnRedo.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/edit-redo.png"))); // NOI18N
        btnRedo.setToolTipText("Redo");
        btnRedo.setEnabled(false);
        btnRedo.setFocusable(false);
        btnRedo.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRedoActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(10, 32767));

        jSeparator7.setOrientation(SwingConstants.VERTICAL);
        jSeparator7.setMaximumSize(new java.awt.Dimension(10, 32767));

        btnCompile.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/compile.png"))); // NOI18N
        btnCompile.setToolTipText("Compile source");
        btnCompile.setFocusable(false);
        btnCompile.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompileActionPerformed(evt);
            }
        });

        toolStandard.add(btnNew);
        toolStandard.add(btnOpen);
        toolStandard.add(btnSave);
        toolStandard.add(jSeparator1);
        toolStandard.add(btnUndo);
        toolStandard.add(btnRedo);
        toolStandard.add(jSeparator2);
        toolStandard.add(btnFindReplace);
        toolStandard.add(btnCut);
        toolStandard.add(btnCopy);
        toolStandard.add(btnPaste);
        toolStandard.add(jSeparator7);
        toolStandard.add(btnCompile);

        splitSoure.setBorder(null);
        splitSoure.setDividerLocation(260);
        splitSoure.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitSoure.setOneTouchExpandable(true);
        splitSoure.setLeftComponent(jScrollPane1);

        txtOutput.setColumns(20);
        txtOutput.setEditable(false);
        txtOutput.setFont(new Font("Monospaced", 0, 12));
        txtOutput.setLineWrap(true);
        txtOutput.setRows(3);
        txtOutput.setWrapStyleWord(true);
        jScrollPane2.setViewportView(txtOutput);

        splitSoure.setRightComponent(jScrollPane2);

        GroupLayout panelSourceLayout = new GroupLayout(panelSource);
        panelSource.setLayout(panelSourceLayout);
        panelSourceLayout.setHorizontalGroup(
                panelSourceLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(toolStandard) //, GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE)
                .addGroup(panelSourceLayout.createSequentialGroup().addContainerGap().addComponent(splitSoure) //, GroupLayout.DEFAULT_SIZE, 708, Short.MAX_VALUE)
                .addContainerGap()));
        panelSourceLayout.setVerticalGroup(
                panelSourceLayout.createSequentialGroup().addComponent(toolStandard, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE).addComponent(splitSoure, 10, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addContainerGap());

        tabbedPane.addTab("Source code editor", panelSource);

        panelEmulator.setOpaque(false);

        splitLeftRight.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitLeftRight.setContinuousLayout(true);
        splitLeftRight.setFocusable(false);

        statusWindow.setBorder(BorderFactory.createTitledBorder("Status"));

        splitLeftRight.setRightComponent(statusWindow);

        splitPerDebug.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        splitPerDebug.setDividerLocation(330);
        splitPerDebug.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPerDebug.setAutoscrolls(true);
        splitPerDebug.setContinuousLayout(true);

        debuggerPanel.setBorder(BorderFactory.createTitledBorder("Debugger"));

        toolDebug.setFloatable(false);
        toolDebug.setRollover(true);
        toolDebug.setBorder(null);
        toolDebug.setBorderPainted(false);

        btnReset.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/reset.png"))); // NOI18N
        btnReset.setToolTipText("Reset emulation");
        btnReset.setFocusable(false);
        btnReset.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        btnBeginning.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/go-first.png"))); // NOI18N
        btnBeginning.setToolTipText("Jump to beginning");
        btnBeginning.setFocusable(false);
        btnBeginning.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBeginningActionPerformed(evt);
            }
        });

        btnBack.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/go-previous.png"))); // NOI18N
        btnBack.setToolTipText("Step back");
        btnBack.setFocusable(false);
        btnBack.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });

        btnStop.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/go-stop.png"))); // NOI18N
        btnStop.setToolTipText("Stop emulation");
        btnStop.setFocusable(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        btnPause.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/go-pause.png"))); // NOI18N
        btnPause.setToolTipText("Pause emulation");
        btnPause.setFocusable(false);
        btnPause.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseActionPerformed(evt);
            }
        });

        btnRun.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/go-play.png"))); // NOI18N
        btnRun.setToolTipText("Run emulation");
        btnRun.setFocusable(false);
        btnRun.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunActionPerformed(evt);
            }
        });

        btnRunTime.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/go-play-time.png"))); // NOI18N
        btnRunTime.setToolTipText("Run emulation in time slices");
        btnRunTime.setFocusable(false);
        btnRunTime.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunTimeActionPerformed(evt);
            }
        });

        btnStep.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/go-next.png"))); // NOI18N
        btnStep.setToolTipText("Step forward");
        btnStep.setFocusable(false);
        btnStep.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStepActionPerformed(evt);
            }
        });

        btnJump.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/go-jump.png"))); // NOI18N
        btnJump.setToolTipText("Jump to address");
        btnJump.setFocusable(false);
        btnJump.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnJumpActionPerformed(evt);
            }
        });

        btnBreakpoint.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/breakpoints.png"))); // NOI18N
        btnBreakpoint.setToolTipText("Set/unset breakpoint to address...");
        btnBreakpoint.setFocusable(false);
        btnBreakpoint.setHorizontalTextPosition(SwingConstants.CENTER);
        btnBreakpoint.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnBreakpoint.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBreakpointActionPerformed(evt);
            }
        });

        btnMemory.setIcon(new ImageIcon(getClass().getResource("/emustudio/resources/grid_memory.gif"))); // NOI18N
        btnMemory.setToolTipText("Show operating memory");
        btnMemory.setFocusable(false);
        btnMemory.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMemoryActionPerformed(evt);
            }
        });
        toolDebug.add(btnReset);
        toolDebug.add(btnBeginning);
        toolDebug.add(btnBack);
        toolDebug.add(btnStop);
        toolDebug.add(btnPause);
        toolDebug.add(btnRun);
        toolDebug.add(btnRunTime);
        toolDebug.add(btnStep);
        toolDebug.add(btnJump);
        toolDebug.add(btnBreakpoint);
        toolDebug.add(btnMemory);

        btnPrevious.setText("< Previous");
        btnPrevious.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPreviousActionPerformed(evt);
            }
        });

        btnNext.setText("Next >");
        btnNext.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextActionPerformed(evt);
            }
        });

        btnToPC.setText("To PC");
        btnToPC.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnToPCActionPerformed(evt);
            }
        });

        GroupLayout debuggerPanelLayout = new GroupLayout(debuggerPanel);
        debuggerPanel.setLayout(debuggerPanelLayout);
        debuggerPanelLayout.setHorizontalGroup(
                debuggerPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(toolDebug) //, GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
                .addGroup(debuggerPanelLayout.createSequentialGroup().addComponent(btnPrevious).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnToPC).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE).addComponent(btnNext)).addComponent(paneDebug, 10, 350, Short.MAX_VALUE));
        debuggerPanelLayout.setVerticalGroup(
                debuggerPanelLayout.createSequentialGroup().addComponent(toolDebug, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE).addComponent(paneDebug, GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(debuggerPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(btnPrevious).addComponent(btnNext).addComponent(btnToPC)));
        splitLeftRight.setDividerLocation(1.0);
        splitPerDebug.setTopComponent(debuggerPanel);

        peripheralPanel.setBorder(BorderFactory.createTitledBorder("Peripheral devices"));

        paneDevices.setViewportView(lstDevices);
        lstDevices.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int i = lstDevices.getSelectedIndex();
                if (i >= 0)
                    btnShowSettings.setEnabled(arch.getDevice(i).isShowSettingsSupported());

                if (e.getClickCount() == 2) {
                    showGUIButtonActionPerformed(new ActionEvent(this, 0, ""));
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });

        btnShowSettings.setText("Settings");
        btnShowSettings.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showSettingsButtonActionPerformed(evt);
            }
        });

        btnShowGUI.setText("Show");
        btnShowGUI.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showGUIButtonActionPerformed(evt);
            }
        });

        GroupLayout peripheralPanelLayout = new GroupLayout(peripheralPanel);
        peripheralPanel.setLayout(peripheralPanelLayout);
        peripheralPanelLayout.setHorizontalGroup(
                peripheralPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(paneDevices).addGroup(GroupLayout.Alignment.TRAILING, peripheralPanelLayout.createSequentialGroup().addContainerGap().addComponent(btnShowSettings).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnShowGUI).addContainerGap()));
        peripheralPanelLayout.setVerticalGroup(
                peripheralPanelLayout.createSequentialGroup().addComponent(paneDevices).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(peripheralPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(btnShowSettings).addComponent(btnShowGUI)));
        splitPerDebug.setRightComponent(peripheralPanel);
        splitLeftRight.setLeftComponent(splitPerDebug);

        GroupLayout panelEmulatorLayout = new GroupLayout(panelEmulator);
        panelEmulator.setLayout(panelEmulatorLayout);
        panelEmulatorLayout.setHorizontalGroup(
                panelEmulatorLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(splitLeftRight));
        panelEmulatorLayout.setVerticalGroup(
                panelEmulatorLayout.createSequentialGroup().addContainerGap().addComponent(splitLeftRight, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addContainerGap());

        tabbedPane.addTab("Emulator", panelEmulator);

        mnuFile.setText("File");

        mnuFileNew.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileNew.setText("New");
        mnuFileNew.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileNewActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileNew);

        mnuFileOpen.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileOpen.setText("Open...");
        mnuFileOpen.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileOpenActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileOpen);
        mnuFile.add(jSeparator3);

        mnuFileSave.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileSave.setText("Save");
        mnuFileSave.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileSaveActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSave);

        mnuFileSaveAs.setText("Save As...");
        mnuFileSaveAs.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileSaveAsActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSaveAs);
        mnuFile.add(jSeparator4);

        mnuFileExit.setText("Exit");
        mnuFileExit.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileExitActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileExit);

        jMenuBar2.add(mnuFile);

        mnuEdit.setText("Edit");

        mnuEditUndo.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditUndo.setText("Undo");
        mnuEditUndo.setEnabled(false);
        mnuEditUndo.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditUndoActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditUndo);

        mnuEditRedo.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditRedo.setText("Redo");
        mnuEditRedo.setEnabled(false);
        mnuEditRedo.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditRedoActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditRedo);
        mnuEdit.add(jSeparator6);

        mnuEditCut.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditCut.setText("Cut selection");
        mnuEditCut.setEnabled(false);
        mnuEditCut.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditCutActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCut);

        mnuEditCopy.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditCopy.setText("Copy selection");
        mnuEditCopy.setEnabled(false);
        mnuEditCopy.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditCopyActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCopy);

        mnuEditPaste.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditPaste.setText("Paste selection");
        mnuEditPaste.setEnabled(false);
        mnuEditPaste.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditPasteActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditPaste);
        mnuEdit.add(jSeparator5);

        mnuEditFind.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditFind.setText("Find/replace text...");
        mnuEditFind.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditFindActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditFind);

        mnuEditFindNext.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        mnuEditFindNext.setText("Find next");
        mnuEditFindNext.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditFindNextActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditFindNext);

        mnuEditReplaceNext.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
        mnuEditReplaceNext.setText("Replace next");
        mnuEditReplaceNext.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditReplaceNextActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditReplaceNext);

        jMenuBar2.add(mnuEdit);

        mnuProject.setText("Project");

        mnuProjectCompile.setText("Compile source...");
        mnuProjectCompile.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuProjectCompileActionPerformed(evt);
            }
        });
        mnuProject.add(mnuProjectCompile);

        mnuProjectViewConfig.setText("View computer...");
        mnuProjectViewConfig.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuProjectViewConfigActionPerformed(evt);
            }
        });
        mnuProject.add(mnuProjectViewConfig);

        mnuProjectCompilerSettings.setText("Compiler settings...");
        mnuProjectCompilerSettings.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuProjectCompilerSettingsActionPerformed(evt);
            }
        });
        mnuProject.add(mnuProjectCompilerSettings);

        jMenuBar2.add(mnuProject);

        mnuHelp.setText("Help");

        mnuHelpAbout.setText("About...");
        mnuHelpAbout.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuHelpAboutActionPerformed(evt);
            }
        });
        mnuHelp.add(mnuHelpAbout);

        jMenuBar2.add(mnuHelp);

        setJMenuBar(jMenuBar2);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));

        pack();
    }

    private void btnPauseActionPerformed(java.awt.event.ActionEvent evt) {
        cpu.pause();
    }

    private void showSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            int i = lstDevices.getSelectedIndex();
            if (i == -1) {
                StaticDialogs.showErrorMessage("Device has to be selected!");
                return;
            }
            arch.getDevices()[i].showSettings();
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Can't show settings of the device:\n "
                    + e.getMessage());
        }
    }

    private void showGUIButtonActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            int i = lstDevices.getSelectedIndex();
            if (i == -1) {
                StaticDialogs.showErrorMessage("Device has to be selected!");
                return;
            }
            arch.getDevices()[i].showGUI();
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Can't show GUI of the device:\n "
                    + e.getMessage());
        }
    }

    private void btnMemoryActionPerformed(java.awt.event.ActionEvent evt) {
        if ((memory != null) && (memory.isShowSettingsSupported())) {
            memory.showSettings();
        } else
            StaticDialogs.showMessage("The GUI is not supported");
    }

    private void btnStepActionPerformed(java.awt.event.ActionEvent evt) {
        cpu.step();
    }

    private void btnRunActionPerformed(java.awt.event.ActionEvent evt) {
        tblDebug.setVisible(false);
        cpu.execute();
    }

    private void btnRunTimeActionPerformed(java.awt.event.ActionEvent evt) {
        String sliceText = StaticDialogs.inputStringValue("Enter time slice in milliseconds:",
                "Run timed emulation", "500");
        try {
            final int slice = Integer.parseInt(sliceText);
            new Thread() {

                @Override
                public void run() {
                    while (run_state == RunState.STATE_STOPPED_BREAK) {
                        cpu.step();
                        try {
                            Thread.sleep(slice);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }.start();
        } catch (NumberFormatException e) {
            StaticDialogs.showErrorMessage("Error: the number has to be integer,");
        }
    }

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {
        cpu.stop();
    }

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            int pc = cpu.getInstrPosition();
            if (pc > 0) {
                cpu.setInstrPosition(pc - 1);
                paneDebug.revalidate();
                if (tblDebug.isVisible()) {
                    tblDebug.revalidate();
                    tblDebug.repaint();
                }
            }
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnBeginningActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            cpu.setInstrPosition(0);
            paneDebug.revalidate();
            if (tblDebug.isVisible()) {
                tblDebug.revalidate();
                tblDebug.repaint();
            }
        } catch (NullPointerException e) {
        }
    }

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {
        if (memory != null) {
            cpu.reset(memory.getProgramStart()); // first address of an image??
            memory.reset();
        } else {
            cpu.reset();
        }
        IDevice dev[] = arch.getDevices();
        if (dev != null) {
            for (int i = 0; i < dev.length; i++) {
                dev[i].reset();
            }
        }
        paneDebug.revalidate();
    }

    private void btnJumpActionPerformed(java.awt.event.ActionEvent evt) {
        int address = 0;
        try {
            address = Integer.decode(StaticDialogs.inputStringValue("Jump to address: ", "Jump", "0")).intValue();
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("The number entered is in"
                    + " inccorret format", "Jump");
            return;
        }
        if (cpu.setInstrPosition(address) == false) {
            String maxSize = (memory != null)
                    ? "\n (expected range from 0 to "
                    + String.valueOf(memory.getSize()) + ")"
                    : "";
            StaticDialogs.showErrorMessage("Typed address is incorrect !"
                    + maxSize, "Jump");
            return;
        }
        paneDebug.revalidate();
        if (tblDebug.isVisible()) {
            tblDebug.revalidate();
            tblDebug.repaint();
        }
    }

    private void mnuHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {
        (new AboutDialog(this, true)).setVisible(true);
    }

    private void mnuProjectCompileActionPerformed(java.awt.event.ActionEvent evt) {
        btnCompileActionPerformed(evt);
    }

    private void btnCompileActionPerformed(java.awt.event.ActionEvent evt) {
        if (run_state == RunState.STATE_RUNNING) {
            StaticDialogs.showErrorMessage("You must first stop running emulation.");
            return;
        }
        if (compiler == null) {
            StaticDialogs.showErrorMessage("Compiler is not defined.");
            return;
        }
        txtOutput.setText("");
        String fn = txtSource.getFileName();
//        fn = fn.substring(0, fn.lastIndexOf(".")) + ".hex"; // chyba.

        try {
            StringReader sourceReader;
            sourceReader = new StringReader(txtSource.getText());
            if (memory != null) {
                memory.reset();
            }
            compiler.compile(fn, sourceReader);
            int programStart = compiler.getProgramStartAddress();
            if (memory != null) {
                memory.setProgramStart(programStart);
            }
            cpu.reset(programStart);
        } catch (Exception e) {
            txtOutput.append(e.toString() + "\n");
        } catch (Error ex) {
            txtOutput.append(ex.toString() + "\n");
        }
    }

    private void mnuProjectViewConfigActionPerformed(java.awt.event.ActionEvent evt) {
        new ViewComputerDialog(this, true).setVisible(true);
    }

    private void mnuProjectCompilerSettingsActionPerformed(java.awt.event.ActionEvent evt) {
        if ((compiler != null) && (compiler.isShowSettingsSupported())) {
            compiler.showSettings();
        }
    }

    private void mnuEditPasteActionPerformed(java.awt.event.ActionEvent evt) {
        btnPasteActionPerformed(evt);
    }

    private void mnuEditCopyActionPerformed(java.awt.event.ActionEvent evt) {
        btnCopyActionPerformed(evt);
    }

    private void mnuEditCutActionPerformed(java.awt.event.ActionEvent evt) {
        btnCutActionPerformed(evt);
    }

    private void mnuEditRedoActionPerformed(java.awt.event.ActionEvent evt) {
        btnRedoActionPerformed(evt);
    }

    private void mnuEditUndoActionPerformed(java.awt.event.ActionEvent evt) {
        btnUndoActionPerformed(evt);
    }

    private void mnuFileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
        txtSource.saveFileDialog();
    }

    private void mnuFileSaveActionPerformed(java.awt.event.ActionEvent evt) {
        btnSaveActionPerformed(evt);
    }

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
        txtSource.saveFile();
    }

    private void mnuFileOpenActionPerformed(java.awt.event.ActionEvent evt) {
        btnOpenActionPerformed(evt);
    }

    private void mnuFileNewActionPerformed(java.awt.event.ActionEvent evt) {
        btnNewActionPerformed(evt);
    }

    private void mnuFileExitActionPerformed(java.awt.event.ActionEvent evt) {
        this.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        if (txtSource.confirmSave() == true) {
            return;
        }
        arch.destroy();
        dispose();
        System.exit(0); //calling the method is a must
    }

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {
        txtSource.openFileDialog();
        txtOutput.setText("");
    }

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {
        txtSource.newFile();
        txtOutput.setText("");
    }

    private void btnFindReplaceActionPerformed(java.awt.event.ActionEvent evt) {
        mnuEditFindActionPerformed(evt);
    }

    private void btnPasteActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            txtSource.paste();
        } catch (Exception e) {
        }
    }

    private void btnCopyActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            txtSource.copy();
        } catch (Exception e) {
        }
    }

    private void btnCutActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            txtSource.cut();
        } catch (Exception e) {
        }
    }

    private void btnRedoActionPerformed(java.awt.event.ActionEvent evt) {
        txtSource.redo();
        //undoStateListener.actionPerformed(new ActionEvent(this,0,""));
    }

    private void btnUndoActionPerformed(java.awt.event.ActionEvent evt) {
        txtSource.undo();
        //undoStateListener.actionPerformed(new ActionEvent(this,0,""));
    }

    private void mnuEditFindActionPerformed(java.awt.event.ActionEvent evt) {
        new FindDialog(this, false, txtSource).setVisible(true);
    }

    private void mnuEditFindNextActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (FindText.getInstance().findNext(txtSource.getText(),
                    txtSource.getCaretPosition(),
                    txtSource.getDocument().getEndPosition().getOffset() - 1)) {
                txtSource.select(FindText.getInstance().getMatchStart(),
                        FindText.getInstance().getMatchEnd());
                txtSource.grabFocus();
            } else {
                StaticDialogs.showMessage("Expression was not found");
            }
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }

    private void btnBreakpointActionPerformed(java.awt.event.ActionEvent evt) {
        int address = 0;
        new BreakpointDialog(this, true).setVisible(true);
        address = BreakpointDialog.getAdr();
        if ((address != -1) && arch.getCPU().isBreakpointSupported()) {
            arch.getCPU().setBreakpoint(address,
                    BreakpointDialog.getSet());
        }
        paneDebug.revalidate();
        if (tblDebug.isVisible()) {
            tblDebug.revalidate();
            tblDebug.repaint();
        }
    }

    private void mnuEditReplaceNextActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (FindText.getInstance().replaceNext(txtSource)) {
                txtSource.grabFocus();
            } else {
                StaticDialogs.showMessage("Expression was not found");
            }
        } catch (NullPointerException e) {
            mnuEditFindActionPerformed(evt);
        }
    }

    private void btnPreviousActionPerformed(java.awt.event.ActionEvent evt) {
        debug_model.previousPage();
        tblDebug.revalidate();
        tblDebug.repaint();
    }

    private void btnToPCActionPerformed(java.awt.event.ActionEvent evt) {
        debug_model.gotoPC();
        tblDebug.revalidate();
        tblDebug.repaint();
    }

    private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {
        debug_model.nextPage();
        tblDebug.revalidate();
        tblDebug.repaint();
    }

    JButton btnBack;
    JButton btnBeginning;
    JButton btnBreakpoint;
    JButton btnFindReplace;
    JButton btnCopy;
    JButton btnCut;
    JButton btnPaste;
    JButton btnPause;
    JButton btnRedo;
    JButton btnRun;
    JButton btnRunTime;
    JButton btnStep;
    JButton btnStop;
    JButton btnUndo;
    JScrollPane jScrollPane1;
    JList lstDevices;
    JMenuItem mnuEditCopy;
    JMenuItem mnuEditCut;
    JMenuItem mnuEditPaste;
    JMenuItem mnuEditRedo;
    JMenuItem mnuEditUndo;
    JScrollPane paneDebug;
    JPanel statusWindow;
    JTextArea txtOutput;
    JMenuItem mnuProjectCompilerSettings;
    JButton btnMemory;
    JButton btnCompile;
    JMenuItem mnuProjectCompile;
}
