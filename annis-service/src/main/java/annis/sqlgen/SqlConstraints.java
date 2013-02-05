/*
 * Copyright 2009-2011 Collaborative Research Centre SFB 632 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.sqlgen;

import java.util.Collection;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import annis.model.QueryNode.TextMatching;

/**
 * TODO: write documentation for BaseSqlClauseGenerator, fix name
 *
 * @author Viktor Rosenfeld <rosenfel@informatik.hu-berlin.de>
 */
public class SqlConstraints
{

  private static boolean disableBetweenSymmetricPredicate = false;
  private static boolean disableBetweenPredicate = false;

  /**
   * Test a column for true.
   *
   * @param lhs The column which should be checked for true.
   */
  public static String isTrue(String lhs)
  {
    return lhs + " IS TRUE";
  }

  /**
   * Test a column for true.
   *
   * @param lhs The column which should be checked for false.
   */
  public static String isFalse(String lhs)
  {
    return lhs + " IS NOT TRUE";
  }

  /**
   * Generate IS NULL predicate on column.
   *
   * @param lhs The column which should be checked for NULL values.
   */
  public static String isNull(String lhs)
  {
    return lhs + " IS NULL";
  }

  /**
   * Generate IS NOT NULL predicate on column.
   *
   * @param lhs The column which should be checked for values that are not NULL.
   */
  public static String isNotNull(String lhs)
  {
    return lhs + " IS NOT NULL";
  }

  public static String join(String op, String lhs, String rhs)
  {
    return lhs + " " + op + " " + rhs;
  }

  public static String numberJoin(String op, String lhs, String rhs, int offset)
  {
    String plus = offset >= 0 ? " + " : " - ";
    return join(op, lhs, rhs) + plus + String.valueOf(Math.abs(offset));
  }

  public static String bitSelect(String column, boolean[] bits)
  {
    StringBuilder sbBits = new StringBuilder();
    for (int i = 0; i < bits.length; i++)
    {
      sbBits.append(bits[i] ? "1" : "0");
    }
    return "(" + column + " & B'" + sbBits.toString() + "') " + "= B'"
      + sbBits.toString() + "'";
  }

  public static String between(String lhs, String rhs, int min, int max)
  {
    if ((disableBetweenSymmetricPredicate || disableBetweenPredicate)
      && min > max)
    {
      int tmp = min;
      min = max;
      max = tmp;
    }
    String minPlus = min >= 0 ? " + " : " - ";
    String maxPlus = max >= 0 ? " + " : " - ";
    if (disableBetweenPredicate)
    {
      return lhs + " >= " + rhs + minPlus + String.valueOf(Math.abs(min))
        + " AND " + lhs + " <= " + rhs + maxPlus
        + String.valueOf(Math.abs(max));
    }
    else
    {
      String betweenPredicate = disableBetweenSymmetricPredicate ? "BETWEEN"
        : "BETWEEN SYMMETRIC";
      return lhs + " " + betweenPredicate + " " + rhs + minPlus
        + String.valueOf(Math.abs(min)) + " AND " + rhs + maxPlus
        + String.valueOf(Math.abs(max));
    }
  }

  public static String between(String lhs, int min, int max)
  {
    if ((disableBetweenSymmetricPredicate || disableBetweenPredicate)
      && min > max)
    {
      int tmp = min;
      min = max;
      max = tmp;
    }
    if (disableBetweenPredicate)
    {
      return lhs + " >= " + min + " AND " + lhs + " <= " + max;
    }
    else
    {
      String betweenPredicate = disableBetweenSymmetricPredicate ? "BETWEEN"
        : "BETWEEN SYMMETRIC";
      return lhs + " " + betweenPredicate + " " + min + " AND " + max;
    }
  }

  public static String in(String lhs, String rhs)
  {
    return lhs + " IN (" + rhs + ")";
  }

  public static String in(String lhs, Collection<?> values)
  {
    if (values.isEmpty())
    {
      return in(lhs, "NULL");
    }
    else
    {
      return in(lhs, StringUtils.join(values, ","));
    }
  }
  
  public static String any(String lhs, String rhs)
  {
    return lhs + " = ANY (" + rhs + ")";
  }

  public static String sqlString(String string)
  {
    if(string == null)
    {
      return "''";
    }
    else
    {
      return "'" + StringUtils.replace(string, "'", "''") + "'";
    }
  }

  public static String sqlString(String string, TextMatching textMatching)
  {
    if (textMatching == TextMatching.REGEXP_EQUAL
      || textMatching == TextMatching.REGEXP_NOT_EQUAL)
    {
      string = "^(" + string + ")$";
    }
    return sqlString(string);
  }

  public static boolean isDisableBetweenSymmetricPredicate()
  {
    return disableBetweenSymmetricPredicate;
  }

  public static void setDisableBetweenSymmetricPredicate(
    boolean disableBetweenPredicate)
  {
    SqlConstraints.disableBetweenSymmetricPredicate = disableBetweenPredicate;
  }

  public static boolean isDisableBetweenPredicate()
  {
    return disableBetweenPredicate;
  }

  public static void setDisableBetweenPredicate(boolean disableBetweenPredicate)
  {
    SqlConstraints.disableBetweenPredicate = disableBetweenPredicate;
  }
}
