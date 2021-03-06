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

package com.foundationdb.sql.server;

import java.sql.ResultSet;
import java.util.Queue;

import com.foundationdb.ais.model.AkibanInformationSchema;
import com.foundationdb.ais.model.CacheValueGenerator;
import com.foundationdb.ais.model.Parameter;
import com.foundationdb.direct.Direct;
import com.foundationdb.direct.DirectClassLoader;
import com.foundationdb.direct.DirectContextImpl;
import com.foundationdb.qp.operator.QueryBindings;
import com.foundationdb.server.explain.Explainable;

/** A Routine that uses Java native data types in its invocation API. */
public abstract class ServerJavaRoutine implements Explainable
{
    private ServerQueryContext context;
    private QueryBindings bindings;
    private ServerRoutineInvocation invocation;

    protected ServerJavaRoutine(ServerQueryContext context,
                                QueryBindings bindings,
                                ServerRoutineInvocation invocation) {
        this.context = context;
        this.bindings = bindings;
        this.invocation = invocation;
    }

    public ServerQueryContext getContext() {
        return context;
    }

    public ServerRoutineInvocation getInvocation() {
        return invocation;
    }

    public abstract void setInParameter(Parameter parameter, ServerJavaValues values, int index);
    public abstract void invoke();
    public abstract Object getOutParameter(Parameter parameter, int index);
    public abstract Queue<ResultSet> getDynamicResultSets();

    public void push() {
        ServerCallContextStack.get().push(context, invocation);
        AkibanInformationSchema ais = context.getServer().getAIS();
        if (context.getServer().isDirectEnabled())
            Direct.enter(context.getCurrentSchema(), ais);
    }

    public void pop(boolean success) {
        if (context.getServer().isDirectEnabled())
            Direct.leave();
        ServerCallContextStack.get().pop(context, invocation, success);
    }

    public void setInputs() {
        int nargs = invocation.size();
        ServerJavaValues values = invocation.asValues(context, bindings);
        for (int i = 0; i < nargs; i++) {
            Parameter parameter = invocation.getRoutineParameter(i);
            switch (parameter.getDirection()) {
            case IN:
            case INOUT:
                setInParameter(parameter, values, i);
                break;
            }
        }
    }

    public void getOutputs() {
        int nargs = invocation.size();
        ServerJavaValues values = invocation.asValues(context, bindings);
        for (int i = 0; i < nargs; i++) {
            Parameter parameter = invocation.getRoutineParameter(i);
            switch (parameter.getDirection()) {
            case INOUT:
            case OUT:
            case RETURN:
                values.setObject(i, getOutParameter(parameter, i));
                break;
            }
        }
        Parameter parameter = invocation.getRoutineParameter(ServerJavaValues.RETURN_VALUE_INDEX);
        if (parameter != null) {
            values.setObject(ServerJavaValues.RETURN_VALUE_INDEX, getOutParameter(parameter, ServerJavaValues.RETURN_VALUE_INDEX));
        }
    }
}
