/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.sql.embedded;

import com.foundationdb.qp.operator.API;
import com.foundationdb.qp.operator.Cursor;
import com.foundationdb.qp.operator.CursorLifecycle;
import com.foundationdb.qp.operator.Operator;
import com.foundationdb.qp.operator.QueryBindings;
import com.foundationdb.qp.operator.RowCursor;
import com.foundationdb.qp.row.ImmutableRow;
import com.foundationdb.qp.row.ProjectedRow;
import com.foundationdb.qp.row.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ExecutableModifyOperatorStatement extends ExecutableOperatorStatement
{
    private static final Logger logger = LoggerFactory.getLogger(ExecutableModifyOperatorStatement.class);

    protected ExecutableModifyOperatorStatement(Operator resultOperator,
                                                JDBCResultSetMetaData resultSetMetaData, 
                                                JDBCParameterMetaData parameterMetaData) {
        super(resultOperator, resultSetMetaData, parameterMetaData);
    }
    
    @Override
    public ExecuteResults execute(EmbeddedQueryContext context, QueryBindings bindings) {
        int updateCount = 0;
        SpoolCursor returningRows = null;
        if (resultSetMetaData != null)
            // If there are results, we need to read them all now to get the update
            // count right and have this all happen even if the caller
            // does not read all of the generated keys.
            returningRows = new SpoolCursor();
        Cursor cursor = null;
        RuntimeException runtimeException = null;
        try {
            cursor = API.cursor(resultOperator, context, bindings);
            cursor.openTopLevel();
            Row row;
            while ((row = cursor.next()) != null) {
                updateCount++;
                if (returningRows != null) {
                    returningRows.add(row);
                }
            }
        }
        catch (RuntimeException ex) {
            runtimeException = ex;
        }
        finally {
            try {
                if (cursor != null) {
                    cursor.destroy();
                }
            }
            catch (RuntimeException ex) {
                if (runtimeException == null)
                    runtimeException = ex;
                else
                    logger.warn("Error cleaning up cursor with exception already pending", ex);
            }
            if (runtimeException != null)
                throw runtimeException;
        }
        if (returningRows != null)
            returningRows.open(); // Done filling.
        return new ExecuteResults(updateCount, returningRows);
    }

    @Override
    public TransactionMode getTransactionMode() {
        return TransactionMode.WRITE;
    }

    @Override
    public TransactionAbortedMode getTransactionAbortedMode() {
        return TransactionAbortedMode.NOT_ALLOWED;
    }

    @Override
    public AISGenerationMode getAISGenerationMode() {
        return AISGenerationMode.NOT_ALLOWED;
    }

    static class SpoolCursor implements RowCursor {
        private List<Row> rows = new ArrayList<>();
        private Iterator<Row> iterator;
        private enum State { CLOSED, FILLING, EMPTYING, DESTROYED }
        private State state;
        
        public SpoolCursor() {
            state = State.FILLING;
        }

        public void add(Row row) {
            assert (state == State.FILLING);
            if (row instanceof ProjectedRow)
                // create a copy of this row, and hold it instead
                row = new ImmutableRow((ProjectedRow)row);
            rows.add(row);
        }

        @Override
        public void open() {
            CursorLifecycle.checkIdle(this);
            iterator = rows.iterator();
            state = State.EMPTYING;
        }        

        @Override
        public Row next() {
            CursorLifecycle.checkIdleOrActive(this);
            if (iterator.hasNext()) {
                Row row = iterator.next();
                return row;
            }
            else {
                close();
                return null;
            }
        }

        @Override
        public void jump(Row row, com.foundationdb.server.api.dml.ColumnSelector columnSelector) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            CursorLifecycle.checkIdleOrActive(this);
            state = State.CLOSED;
        }
        
        @Override
        public void destroy()
        {
            close();
            state = State.DESTROYED;
        }

        @Override
        public boolean isIdle()
        {
            return ((state == State.CLOSED) || (state == State.FILLING));
        }

        @Override
        public boolean isActive()
        {
            return (state == State.EMPTYING);
        }

        @Override
        public boolean isDestroyed()
        {
            return (state == State.DESTROYED);
        }
    }

}


