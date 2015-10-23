package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.ColumnConstraint;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.Table;
import com.google.common.base.Joiner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Interval;

public class TableGenerator {
  private final File outputDirectory;

  public TableGenerator(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  Table<ParserRuleContext> generateTable(SQLiteParser.ParseContext parseContext) {
    if (!parseContext.error().isEmpty()) {
      throw new IllegalStateException("Error: " + parseContext.error(0).toString());
    }

    Table<ParserRuleContext> table = null;
    List<SqlStmt<ParserRuleContext>> sqlStmts = new ArrayList<>();

    for (SQLiteParser.Sql_stmtContext sqlStatement : parseContext.sql_stmt_list(0).sql_stmt()) {
      if (sqlStatement.create_table_stmt() != null) {
        table = tableFor(packageName(parseContext), sqlStatement.create_table_stmt());
      }
      if (sqlStatement.IDENTIFIER() != null) {
        sqlStmts.add(new SqlStmt<>(sqlStatement.IDENTIFIER().getText(), getFullText(
            (ParserRuleContext) sqlStatement.getChild(sqlStatement.getChildCount() - 1)),
            sqlStatement));
      }
    }

    if (table != null) {
      sqlStmts.forEach(table::addSqlStmt);
    }
    return table;
  }

  private Table<ParserRuleContext> tableFor(String packageName,
      SQLiteParser.Create_table_stmtContext createTable) {
    Table<ParserRuleContext> table =
        new Table<>(packageName, createTable.table_name().getText(), createTable, outputDirectory);
    for (SQLiteParser.Column_defContext column : createTable.column_def()) {
      table.addColumn(columnFor(column));
    }
    return table;
  }

  private Column<ParserRuleContext> columnFor(SQLiteParser.Column_defContext column) {
    String columnName = column.column_name().getText();
    Column.Type type = Column.Type.valueOf(column.type_name().getText());
    Column<ParserRuleContext> result = new Column<>(columnName, type, column);
    for (SQLiteParser.Column_constraintContext constraintNode : column.column_constraint()) {
      ColumnConstraint<ParserRuleContext> constraint = constraintFor(constraintNode);
      if (constraint != null) result.columnConstraints.add(constraint);
    }
    return result;
  }

  private ColumnConstraint<ParserRuleContext> constraintFor(
      SQLiteParser.Column_constraintContext constraint) {
    // TODO: Handle constraints
    return null;
  }

  private String packageName(SQLiteParser.ParseContext parseContext) {
    return Joiner.on('.')
        .join(parseContext.package_stmt(0).name().stream().map(RuleContext::getText).iterator());
  }

  private String getFullText(ParserRuleContext context) {
    if (context.start == null
        || context.stop == null
        || context.start.getStartIndex() < 0
        || context.stop.getStopIndex() < 0) {
      return context.getText(); // Fallback
    }

    return context.start.getInputStream().getText(
        Interval.of(context.start.getStartIndex(), context.stop.getStopIndex()));
  }
}
