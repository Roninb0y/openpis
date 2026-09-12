package com.openpis.material.dto;

import lombok.Data;

@Data
public class MaterialUpdateRequest {
    private String title;
    private String description;
    private Integer duration;
}
