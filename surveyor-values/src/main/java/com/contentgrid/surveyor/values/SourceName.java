package com.contentgrid.surveyor.values;

import lombok.Value;
import lombok.experimental.Accessors;

@Value(staticConstructor = "of")
@Accessors(fluent = true)
public class SourceName {
    String sourceName;
}
