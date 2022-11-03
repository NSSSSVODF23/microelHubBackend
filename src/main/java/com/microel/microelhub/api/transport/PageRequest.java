package com.microel.microelhub.api.transport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageRequest {
    private Long offset;
    private Integer limit;
}
