package com.vinodkrishnan.expenses.tasks;

import java.util.List;
import java.util.Map;

public interface GetRowsListener {
    boolean onGetRowsCompleted(List<Map<String, String>> rows);
}

