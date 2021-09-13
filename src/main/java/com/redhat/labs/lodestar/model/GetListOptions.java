package com.redhat.labs.lodestar.model;

import javax.ws.rs.*;

import io.quarkus.panache.common.*;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetListOptions extends GetOptions {

    @Parameter(name = "page", description = "0 based index of page of results to return")
    @QueryParam("page")
    private int page;

    @Parameter(name = "pageSize", description = "number of results to return per page")
    @QueryParam("pageSize")
    private int pageSize;

    @DefaultValue("updated|DESC")
    @Parameter(name = "sort", description = "Sort. Comma separated list field|direction,field|direction. UUID will always be added at the end. Default direction is ASC")
    @QueryParam("sort")
    private String sort;

    public int getPage() {
        return page < 0 ? 0 : page;
    }

    public int getPageSize() {
        return pageSize < 1 ? 20 : pageSize;
    }

    public Sort getQuerySort() {
        return getQuerySort(Sort.by("uuid"));
    }

    public Sort getQuerySort(Sort defaultSort) {
        if(sort == null) {
            return defaultSort;
        }
        String[] sortAll = sort.split(",");
        Sort sort = null;
        String direction;

        for (String s : sortAll) {
            String[] sortFields = s.split("\\|");
            direction = sortFields.length == 2 ? sortFields[1] : "";
            if (sort == null) {
                sort = Sort.by(sortFields[0], getDirection(direction));
            } else {
                sort.and(sortFields[0], getDirection(direction));
            }
        }

        sort.and("uuid");

        return sort;
    }

    private Sort.Direction getDirection(String dir) {
        if("DESC".equals(dir)) {
            return Sort.Direction.Descending;
        }

        return Sort.Direction.Ascending;
    }

}
