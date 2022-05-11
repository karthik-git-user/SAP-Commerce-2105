package com.mirakl.hybris.core.util.flexiblesearch.impl;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

public class SelectClause extends AbstractQueryComponent {

  private boolean count;
  private List<Field> selectFields;

  public SelectClause() {
    selectFields = new ArrayList<>();
  }

  public void setCount(boolean count) {
    this.count = count;
  }

  public void addSelectField(Field field) {
    selectFields.add(field);
  }

  public boolean isCount() {
    return count;
  }

  public List<Field> getSelectFields() {
    return selectFields;
  }

  @Override
  protected StringBuilder append(StringBuilder str) {
    str.append("SELECT ");
    if (count) {
      str.append("COUNT(");
    }
    str.append("{") //
        .append(Joiner.on("}, {").join(selectFields)) //
        .append("}");
    if (count) {
      str.append(")");
    }

    return str;
  }

}
