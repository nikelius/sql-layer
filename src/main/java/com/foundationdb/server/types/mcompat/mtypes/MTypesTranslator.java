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

package com.foundationdb.server.types.mcompat.mtypes;

import com.foundationdb.server.error.UnknownDataTypeException;
import com.foundationdb.server.types.TClass;
import com.foundationdb.server.types.TInstance;
import com.foundationdb.server.types.common.types.TypesTranslator;

import com.foundationdb.sql.types.DataTypeDescriptor;
import com.foundationdb.sql.types.TypeId;

import java.sql.Types;

public class MTypesTranslator extends TypesTranslator
{
    public static final TypesTranslator INSTANCE = new MTypesTranslator();

    private MTypesTranslator() {
    }

    @Override
    public TClass typeForJDBCType(int jdbcType) {
        switch (jdbcType) {
        case Types.BIGINT:
            return MNumeric.BIGINT;
        case Types.BINARY:
        case Types.BIT:
            return MBinary.BINARY;
        case Types.BLOB:
            return MBinary.VARBINARY;
        case Types.CHAR:
        case Types.NCHAR:
            return MString.VARCHAR; // TODO: Should probably be MString.CHAR.
        case Types.CLOB:
        case Types.NCLOB:
            return MString.TEXT;
        case Types.DATE:
            return MDatetimes.DATE;
        case Types.DECIMAL:
        case Types.NUMERIC:
            return MNumeric.DECIMAL;
        case Types.DOUBLE:
            return MApproximateNumber.DOUBLE;
        case Types.FLOAT:
        case Types.REAL:
            return MApproximateNumber.FLOAT;
        case Types.INTEGER:
            return MNumeric.INT;
        case Types.LONGVARBINARY:
            return MBinary.VARBINARY;
        case Types.LONGNVARCHAR:
        case Types.SQLXML:
            return MString.LONGTEXT;
        case Types.LONGVARCHAR:
            return MString.TEXT;
        case Types.SMALLINT:
            return MNumeric.SMALLINT;
        case Types.TIME:
            return MDatetimes.TIME;
        case Types.TIMESTAMP:
            return MDatetimes.TIMESTAMP;
        case Types.TINYINT:
            return MNumeric.TINYINT;
        case Types.VARBINARY:
            return MBinary.VARBINARY;
        case Types.VARCHAR:
        case Types.NVARCHAR:
            return MString.VARCHAR;
        case Types.ARRAY:
        case Types.BOOLEAN:
        case Types.DATALINK:
        case Types.DISTINCT:
        case Types.JAVA_OBJECT:
        case Types.NULL:
        case Types.OTHER:
        case Types.REF:
        case Types.ROWID:
        case Types.STRUCT:
        default:
            throw new UnknownDataTypeException(jdbcTypeName(jdbcType));
        }
    }

    @Override
    protected TInstance toTInstance(TypeId typeId, DataTypeDescriptor sqlType) {
        // Handle non-standard cases.
        switch (typeId.getTypeFormatId()) {
        case TypeId.FormatIds.TINYINT_TYPE_ID:
            if (typeId.isUnsigned()) {
                return MNumeric.TINYINT_UNSIGNED.instance(sqlType.isNullable());
            }
            break;
        case TypeId.FormatIds.SMALLINT_TYPE_ID:
            if (typeId == TypeId.YEAR_ID) {
                return MDatetimes.YEAR.instance(sqlType.isNullable());
            }
            else if (typeId.isUnsigned()) {
                return MNumeric.SMALLINT_UNSIGNED.instance(sqlType.isNullable());
            }
            break;
        case TypeId.FormatIds.MEDIUMINT_ID:
            if (typeId.isUnsigned()) {
                return MNumeric.MEDIUMINT_UNSIGNED.instance(sqlType.isNullable());
            }
            else {
                return MNumeric.MEDIUMINT.instance(sqlType.isNullable());
            }
        case TypeId.FormatIds.INT_TYPE_ID:
            if (typeId.isUnsigned()) {
                return MNumeric.INT_UNSIGNED.instance(sqlType.isNullable());
            }
            break;
        case TypeId.FormatIds.LONGINT_TYPE_ID:
            if (typeId.isUnsigned()) {
                return MNumeric.BIGINT_UNSIGNED.instance(sqlType.isNullable());
            }
            break;
        case TypeId.FormatIds.REAL_TYPE_ID:
            if (typeId.isUnsigned()) {
                return MApproximateNumber.FLOAT_UNSIGNED.instance(sqlType.isNullable());
            }
            break;
        case TypeId.FormatIds.DOUBLE_TYPE_ID:
            if (typeId.isUnsigned()) {
                return MApproximateNumber.DOUBLE_UNSIGNED.instance(sqlType.isNullable());
            }
            break;
        case TypeId.FormatIds.TIMESTAMP_TYPE_ID:
            if (typeId == TypeId.DATETIME_ID) {
                return MDatetimes.DATETIME.instance(sqlType.isNullable());
            }
            break;
        case TypeId.FormatIds.USERDEFINED_TYPE_ID:
            {
                // TODO: Are there any left that hit this clause?
                String name = typeId.getSQLTypeName();
                for (com.foundationdb.ais.model.Type aisType : com.foundationdb.ais.model.Types.types()) {
                    if (aisType.name().equalsIgnoreCase(name)) {
                        return com.foundationdb.ais.model.Column.generateTInstance(null, aisType, null, null, 
                                                        sqlType.isNullable());
                    }
                }
            }
        }
        return super.toTInstance(typeId, sqlType);
    }
    
    @Override
    public Class<?> jdbcClass(TInstance tinstance) {
        TClass tclass = TInstance.tClass(tinstance);
        if (tclass == MDatetimes.DATE)
            return java.sql.Date.class;
        if ((tclass == MDatetimes.TIMESTAMP) ||
            (tclass == MDatetimes.DATETIME))
            return java.sql.Timestamp.class;
        if ((tclass == MNumeric.DECIMAL) ||
            (tclass == MNumeric.DECIMAL_UNSIGNED))
            return java.math.BigDecimal.class;
        if ((tclass == MApproximateNumber.DOUBLE) ||
            (tclass == MApproximateNumber.DOUBLE_UNSIGNED))
            return Double.class;
        if ((tclass == MApproximateNumber.FLOAT) ||
            (tclass == MApproximateNumber.FLOAT_UNSIGNED))
            return Float.class;
        if (tclass == MNumeric.TINYINT)
            return Byte.class;
        if ((tclass == MNumeric.TINYINT_UNSIGNED) ||
            (tclass == MNumeric.SMALLINT) ||
            (tclass == MDatetimes.YEAR))
            return Short.class;
        if ((tclass == MNumeric.SMALLINT_UNSIGNED) ||
            (tclass == MNumeric.INT) ||
            (tclass == MNumeric.MEDIUMINT))
            return Integer.class;
        if ((tclass == MNumeric.INT_UNSIGNED) ||
            (tclass == MNumeric.BIGINT))
            return Long.class;
        if (tclass == MNumeric.BIGINT_UNSIGNED)
            return java.math.BigInteger.class;
        if ((tclass == MString.CHAR) ||
            (tclass == MString.VARCHAR) ||
            (tclass == MString.TINYTEXT) ||
            (tclass == MString.MEDIUMTEXT) ||
            (tclass == MString.TEXT) ||
            (tclass == MString.LONGTEXT))
            return String.class;
        if (tclass == MDatetimes.TIME)
            return java.sql.Time.class;
        if ((tclass == MBinary.VARBINARY) ||
            (tclass == MBinary.BINARY) ||
            (tclass == MBinary.TINYBLOB) ||
            (tclass == MBinary.MEDIUMBLOB) ||
            (tclass == MBinary.BLOB) ||
            (tclass == MBinary.LONGBLOB))
            return byte[].class;
        return super.jdbcClass(tinstance);
    }

    @Override
    public boolean isTypeSigned(TInstance tinstance) {
        TClass tclass = TInstance.tClass(tinstance);
        return ((tclass == MNumeric.TINYINT) ||
                (tclass == MNumeric.SMALLINT) ||
                (tclass == MNumeric.MEDIUMINT) ||
                (tclass == MNumeric.INT) ||
                (tclass == MNumeric.BIGINT) ||
                (tclass == MNumeric.DECIMAL) ||
                (tclass == MApproximateNumber.DOUBLE) ||
                (tclass == MApproximateNumber.FLOAT));
    }

}
