/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.qp.physicaloperator;

import com.akiban.ais.model.GroupTable;
import com.akiban.ais.model.Index;
import com.akiban.qp.expression.Expression;
import com.akiban.qp.expression.IndexKeyRange;
import com.akiban.qp.row.RowBase;
import com.akiban.qp.rowtype.RowType;
import com.akiban.qp.rowtype.Schema;

import java.util.Collection;
import java.util.List;

public class API
{
    public static PhysicalOperator flatten_HKeyOrdered(PhysicalOperator inputOperator,
                                                       RowType parentType,
                                                       RowType childType)
    {
        return flatten_HKeyOrdered(inputOperator, parentType, childType, Flatten_HKeyOrdered.DEFAULT);
    }

    public static PhysicalOperator flatten_HKeyOrdered(PhysicalOperator inputOperator,
                                                       RowType parentType,
                                                       RowType childType,
                                                       int flags)
    {
        return new Flatten_HKeyOrdered(inputOperator, parentType, childType, flags);
    }

    public static PhysicalOperator groupScan_Default(GroupTable groupTable)
    {
        return groupScan_Default(groupTable, false, NO_LIMIT);
    }

    public static PhysicalOperator groupScan_Default(GroupTable groupTable,
                                                     boolean reverse,
                                                     Limit limit,
                                                     IndexKeyRange indexKeyRange)
    {
        return new GroupScan_Default(groupTable, reverse, limit, indexKeyRange);
    }

    public static PhysicalOperator groupScan_Default(GroupTable groupTable, boolean reverse, Limit limit)
    {
        return new GroupScan_Default(groupTable, reverse, limit, null);
    }

    public static PhysicalOperator lookup_Default(PhysicalOperator inputOperator,
                                                  GroupTable groupTable,
                                                  RowType inputRowType,
                                                  RowType outputRowType)
    {
        return lookup_Default(inputOperator, groupTable, inputRowType, outputRowType, NO_LIMIT);
    }

    public static PhysicalOperator lookup_Default(PhysicalOperator inputOperator,
                                                  GroupTable groupTable,
                                                  RowType inputRowType,
                                                  RowType outputRowType,
                                                  Limit limit)
    {
        return new Lookup_Default(inputOperator,
                                  groupTable,
                                  inputRowType,
                                  outputRowType,
                                  limit);
    }

    public static PhysicalOperator ancestorLookup_Default(PhysicalOperator inputOperator,
                                                          GroupTable groupTable,
                                                          RowType rowType,
                                                          List<RowType> ancestorTypes)
    {
        return new AncestorLookup_Default(inputOperator, groupTable, rowType, ancestorTypes);
/*
        assert ancestorTypes.size() == 1;
        return new Lookup_Default(inputOperator,
                                  groupTable,
                                  rowType,
                                  ancestorTypes.get(0),
                                  false,
                                  NO_LIMIT);
*/
    }

    public static PhysicalOperator indexScan_Default(Index index)
    {
        return indexScan_Default(index, false, null);
    }

    public static PhysicalOperator indexScan_Default(Index index, boolean reverse, IndexKeyRange indexKeyRange)
    {
        return new IndexScan_Default(index, reverse, indexKeyRange);
    }

    public static PhysicalOperator select_HKeyOrdered(PhysicalOperator inputOperator,
                                                      RowType predicateRowType,
                                                      Expression predicate)
    {
        return new Select_HKeyOrdered(inputOperator, predicateRowType, predicate);
    }

    public static PhysicalOperator cut_Default(Schema schema,
                                               PhysicalOperator inputOperator,
                                               Collection<RowType> cutTypes)
    {
        return new Cut_Default(schema, inputOperator, cutTypes);
    }

    public static PhysicalOperator extract_Default(Schema schema,
                                                   PhysicalOperator inputOperator,
                                                   Collection<RowType> extractTypes)
    {
        return new Extract_Default(schema, inputOperator, extractTypes);
    }

    private static final Limit NO_LIMIT = new Limit()
    {

        @Override
        public boolean limitReached(RowBase row)
        {
            return false;
        }

        @Override
        public String toString()
        {
            return "NO LIMIT";
        }

    };

    public static Cursor cursor(PhysicalOperator root, StoreAdapter adapter) {
        // if all they need is the wrapped cursor, create it directly
        return new TopLevelWrappingCursor(root.cursor(adapter));
    }
}