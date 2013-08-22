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


package com.foundationdb.server.expression.std;

import com.foundationdb.qp.operator.QueryContext;
import com.foundationdb.server.error.InconvertibleTypesException;
import com.foundationdb.server.error.InvalidCharToNumException;
import com.foundationdb.server.error.WrongExpressionArityException;
import com.foundationdb.server.expression.Expression;
import com.foundationdb.server.expression.ExpressionComposer;
import com.foundationdb.server.expression.ExpressionEvaluation;
import com.foundationdb.server.expression.ExpressionType;
import com.foundationdb.server.service.functions.Scalar;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types.ValueSource;
import com.foundationdb.server.types.extract.Extractors;
import com.foundationdb.server.types.util.ValueHolder;
import com.foundationdb.sql.StandardException;
import com.foundationdb.server.expression.TypesList;
import java.math.BigInteger;
import org.slf4j.LoggerFactory;

public class UnaryBitExpression extends AbstractUnaryExpression
{
    public static enum UnaryBitOperator 
    {
        NOT
        {
            @Override
            public ValueSource exc (BigInteger arg, ValueHolder valueHolder)
            {
                valueHolder.putUBigInt(arg.not().and(n64));
                return valueHolder;
            }
            
            @Override
            public ValueSource errorCase (ValueHolder valueHolder)
            {
                valueHolder.putUBigInt(n64);
                return valueHolder;
            }
        },
        
        COUNT
        {
            @Override
            public ValueSource exc (BigInteger arg, ValueHolder valueHolder)
            {
                valueHolder.putLong(arg.signum() >= 0 ?  arg.bitCount() : 64 - arg.bitCount());
                return valueHolder;
            }
            
            @Override
            public ValueSource errorCase (ValueHolder valueHolder)
            {
                valueHolder.putLong(0L);
                return valueHolder;
            }
        };

        protected abstract ValueSource exc (BigInteger arg, ValueHolder valueHolder);
        protected abstract ValueSource errorCase (ValueHolder valueHolder);
        private static final BigInteger n64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);
   }
    
    @Scalar("bitnot")
    public static final ExpressionComposer NOT_COMPOSER = new InternalComposer(UnaryBitOperator.NOT);
    
    @Scalar("bit_count")
    public static final ExpressionComposer BIT_COUNT_COMPOSER = new InternalComposer(UnaryBitOperator.COUNT);
    
    private static class InternalComposer extends UnaryComposer
    {
        private final UnaryBitOperator op;
        public InternalComposer (UnaryBitOperator op)
        {
            this.op = op;
        }

        @Override
        protected Expression compose(Expression argument, ExpressionType argType, ExpressionType resultType) 
        {
            return new UnaryBitExpression(argument, op);
        }

        @Override
        public ExpressionType composeType(TypesList argumentTypes) throws StandardException
        {
            if (argumentTypes.size() != 1)
                throw new WrongExpressionArityException(1, argumentTypes.size());

            return op == UnaryBitOperator.COUNT ? ExpressionTypes.LONG : ExpressionTypes.U_BIGINT;
        }
    }
            
    private static class InnerEvaluation extends AbstractUnaryExpressionEvaluation
    {
        private final UnaryBitOperator op;
        private static final org.slf4j.Logger log = LoggerFactory.getLogger(UnaryBitExpression.class);
        
        public InnerEvaluation (ExpressionEvaluation ev, UnaryBitOperator op)
        {
            super(ev);
            this.op = op;
        }       

        @Override
        public ValueSource eval() 
        {   
            BigInteger arg;
            try
            {
                arg = Extractors.getUBigIntExtractor().getObject(operand());
            }
            catch (InconvertibleTypesException ex) 
            {
                QueryContext context = queryContext();
                if (context != null)
                    context.warnClient(ex);
                return op.errorCase(valueHolder());
            } 
            catch (NumberFormatException ex)
            {
                QueryContext context = queryContext();
                if (context != null)
                    context.warnClient(new InvalidCharToNumException(ex.getLocalizedMessage()));
                return op.errorCase(valueHolder());
            }           
            return op.exc(arg, valueHolder());
        }        
    }
    
    private final UnaryBitOperator op;
    
    public UnaryBitExpression (Expression ex, UnaryBitOperator op)
    {
        super(op == UnaryBitOperator.COUNT ? AkType.LONG : AkType.U_BIGINT, ex);
        this.op = op;
    }

    @Override
    public String name()
    {
        return op.name();
    }

    @Override
    public ExpressionEvaluation evaluation() 
    {
        if (operand().valueType() == AkType.NULL) return LiteralExpression.forNull().evaluation();
        return new InnerEvaluation(operandEvaluation(), op);
    }    
}