package com.example.carshop.controller;
import com.example.carshop.exception.ApiException;
import org.springframework.data.domain.*;
import java.util.Set;
final class PageRequests {
    private PageRequests() {}
    static Pageable checked(Pageable pageable, String... allowed) {
        var properties = Set.of(allowed);
        for (var order : pageable.getSort()) {
            if (!properties.contains(order.getProperty())) throw ApiException.badRequest("Unsupported sort field: " + order.getProperty());
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("id").ascending();
        if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
    }
}
