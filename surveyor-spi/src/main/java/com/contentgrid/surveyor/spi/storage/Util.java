package com.contentgrid.surveyor.spi.storage;

import java.util.List;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;

@UtilityClass
class Util {
    public <T> T onlyValue(List<T> list, Supplier<T> defaultValue) {
        return switch (list.size()) {
            case 0 -> defaultValue.get();
            case 1 -> list.get(0);
            default -> throw new IllegalStateException("More than one value in the list");
        };
    }

}
