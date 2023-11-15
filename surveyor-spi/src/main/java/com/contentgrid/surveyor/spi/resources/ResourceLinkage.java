package com.contentgrid.surveyor.spi.resources;

import lombok.Value;

@Value
public class ResourceLinkage {
    String applicationRef;
    String projectRef;
    String orgRef;
}
