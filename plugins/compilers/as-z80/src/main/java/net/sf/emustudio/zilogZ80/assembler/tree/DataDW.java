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
package net.sf.emustudio.zilogZ80.assembler.tree;

import emulib.runtime.HEXFileManager;
import net.sf.emustudio.zilogZ80.assembler.exceptions.ValueTooBigException;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.DataValue;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;

public class DataDW extends DataValue {

    private Expression expression = null;

    public DataDW(Expression expr, int line, int column) {
        super(line, column);
        this.expression = expr;
    }

    @Override
    public int getSize() {
        return 2;
    }

    @Override
    public void pass1() {
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        expression.eval(env, addr_start);
        return addr_start + 2;
    }

    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        if (expression.getValue() > 0xFFFF) {
            throw new ValueTooBigException(line, column, expression.getValue(), 0xFFFF);
        }
        if (expression.getValue() < -32768) {
            throw new ValueTooBigException(line, column, expression.getValue(), -32768);
        }

        hex.putCode(expression.encodeValue(2));
    }
}
