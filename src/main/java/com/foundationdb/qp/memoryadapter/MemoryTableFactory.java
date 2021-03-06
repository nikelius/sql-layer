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

package com.foundationdb.qp.memoryadapter;

import com.foundationdb.ais.model.Index;
import com.foundationdb.ais.model.TableName;
import com.foundationdb.qp.expression.IndexKeyRange;
import com.foundationdb.qp.operator.API;
import com.foundationdb.qp.operator.Cursor;
import com.foundationdb.qp.operator.IndexScanSelector;
import com.foundationdb.server.service.session.Session;
import com.foundationdb.server.store.statistics.IndexStatistics;

import static com.foundationdb.qp.memoryadapter.MemoryGroupCursor.GroupScan;

public interface MemoryTableFactory {
    public TableName getName();
    
    // Used by MemoryAdapter to get cursors
    public GroupScan getGroupScan(MemoryAdapter adapter);

    public Cursor getIndexCursor(Index index, Session session,  IndexKeyRange keyRange,
                                 API.Ordering ordering, IndexScanSelector scanSelector);
    
    // Used by IndexStatistics to compute index statistics
    public long rowCount();
    
    // This should return null for all indexes
    // TODO: describe index implementation on memory tables. 
    public IndexStatistics computeIndexStatistics(Session session, Index index);
}
