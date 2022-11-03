package com.microel.microelhub.storage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetRequest implements Pageable {

    private Long offset;
    private Integer limit;
    private Sort sort = Sort.unsorted();

    public OffsetRequest(Long offset, Integer limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public OffsetRequest(Long offset, Integer limit, Sort sort) {
        this.offset = offset;
        this.limit = limit;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return (int) Math.floorDiv(offset, limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetRequest(offset + limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        if (hasPrevious()) {
            return new OffsetRequest(offset - limit, limit, sort);
        } else {
            return first();
        }
    }

    @Override
    public Pageable first() {
        return new OffsetRequest(0L, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetRequest((long) pageNumber * (long) limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
