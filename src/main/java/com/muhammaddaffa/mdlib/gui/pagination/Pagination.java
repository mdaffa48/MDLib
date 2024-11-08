package com.muhammaddaffa.mdlib.gui.pagination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pagination {

    private final Map<Integer, List<?>> pageItems = new HashMap<>();

    public int totalPage = 1;
    public int currentPage = 1;

    public Pagination(List<?> items, int maxItemsPerPage) {
        if (items == null || maxItemsPerPage <= 0) {
            throw new IllegalArgumentException("Invalid input list or maxItemsPerPage");
        }

        // If the items is empty
        if (items.isEmpty()) {
            pageItems.put(1, new ArrayList<>());
            return;
        }

        int totalItems = items.size();
        totalPage = (int) Math.ceil((double) totalItems / maxItemsPerPage);

        for (int i = 0; i < totalPage; i++) {
            int start = i * maxItemsPerPage;
            int end = Math.min(start + maxItemsPerPage, totalItems);
            List<?> pageContent = items.subList(start, end);
            pageItems.put(i + 1, new ArrayList<>(pageContent));
        }
    }

    public boolean nextPage() {
        return this.pageItems.get(this.currentPage + 1) != null;
    }

    public boolean previousPage() {
        return this.pageItems.get(this.currentPage - 1) != null;
    }

    public List<?> getItems() {
        return this.pageItems.get(this.currentPage);
    }

}
